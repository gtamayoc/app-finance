package com.gtc.app_finance.data.repository

import android.util.Log
import com.gtc.app_finance.data.dao.CreditDao
import com.gtc.app_finance.data.dao.PaymentDao
import com.gtc.app_finance.data.dao.SyncQueueDao
import com.gtc.app_finance.data.dao.TransactionDao
import com.gtc.app_finance.data.database.TursoConfigProvider
import com.gtc.app_finance.data.database.TursoSyncClient
import com.gtc.app_finance.data.entity.CreditEntity
import com.gtc.app_finance.data.entity.PaymentEntity
import com.gtc.app_finance.data.entity.SyncQueueEntity
import com.gtc.app_finance.data.entity.TransactionEntity
import com.gtc.app_finance.domain.model.ConnectionStatus
import com.gtc.app_finance.domain.model.Credit
import com.gtc.app_finance.domain.model.FinancialSummary
import com.gtc.app_finance.domain.model.FullDatabaseStatus
import com.gtc.app_finance.domain.model.LocalDbDiagnostic
import com.gtc.app_finance.domain.model.Payment
import com.gtc.app_finance.domain.model.Transaction
import com.gtc.app_finance.domain.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val creditDao: CreditDao,
    private val paymentDao: PaymentDao,
    private val syncQueueDao: SyncQueueDao,
    private val tursoSyncClient: TursoSyncClient,
    private val configProvider: TursoConfigProvider
) {

    private val syncMutex = Mutex()
    private var lastSyncTimestamp = 0L
    private val MIN_SYNC_INTERVAL_MS = 4_000L
    private var hasPurgedLegacySampleData = false

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _credits = MutableStateFlow<List<Credit>>(emptyList())
    val credits: StateFlow<List<Credit>> = _credits.asStateFlow()

    private val _summary = MutableStateFlow(FinancialSummary())
    val summary: StateFlow<FinancialSummary> = _summary.asStateFlow()

    private val _dbStatus = MutableStateFlow(FullDatabaseStatus())
    val dbStatus: StateFlow<FullDatabaseStatus> = _dbStatus.asStateFlow()

    init {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                refreshLocalData()
            } catch (_: Exception) {
                // Safe fallback during testing or before database creation
            }
        }
    }

    suspend fun refreshData(): Unit = withContext(Dispatchers.IO) {
        purgeLegacySampleData()
        refreshLocalData()
        syncData(force = true)
    }

    suspend fun refreshLocalData(): Unit = withContext(Dispatchers.IO) {
        val txEntities = transactionDao.getAll()
        val crEntities = creditDao.getAll()

        val txList = txEntities.map { it.toDomain() }
        val crList = crEntities.map { it.toDomain() }

        _transactions.value = txList
        _credits.value = crList

        calculateSummary(txList, crList)
        updateLocalHealthStatus()
    }

    suspend fun syncData(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val now = System.currentTimeMillis()
            val hasPendingQueue = syncQueueDao.getCount() > 0
            if (!force && !hasPendingQueue && (now - lastSyncTimestamp) < MIN_SYNC_INTERVAL_MS) {
                Log.d("FinanceRepository", "Sync throttled: last sync was ${now - lastSyncTimestamp}ms ago and queue is empty.")
                return@withLock true
            }

            _dbStatus.value = _dbStatus.value.copy(
                remote = _dbStatus.value.remote.copy(status = ConnectionStatus.CONNECTING)
            )

            // 1. Verify connection
            val remoteDiag = tursoSyncClient.testConnection()
            if (remoteDiag.status != ConnectionStatus.CONNECTED) {
                _dbStatus.value = _dbStatus.value.copy(remote = remoteDiag)
                return@withLock false
            }

            // 1.1 Auto-provision remote schema if missing
            val requiredTables = setOf("transactions", "credits", "payments")
            val missingTables = requiredTables - remoteDiag.tablesVerified.toSet()
            if (missingTables.isNotEmpty()) {
                Log.i("FinanceRepository", "Remote database missing tables: $missingTables. Initializing remote schema...")
                val schemaOk = tursoSyncClient.initializeRemoteDatabase()
                if (!schemaOk) {
                    Log.e("FinanceRepository", "Failed to initialize remote schema.")
                    return@withLock false
                }
            }

            // 2. Push any pending changes from the local offline queue
            val pendingQueue = syncQueueDao.getAll()
            for (item in pendingQueue) {
                val success = tursoSyncClient.executeQuery(item.sqlCommand)
                if (success) {
                    syncQueueDao.deleteById(item.id)
                } else {
                    Log.w("FinanceRepository", "Failed to sync pending item ${item.id}: ${item.sqlCommand}")
                }
            }

            // 3. Pull latest remote state from Turso Cloud
            val remoteData = tursoSyncClient.pullRemoteData()
            if (remoteData != null) {
                val pendingTxInsertIds = syncQueueDao.getPendingEntityIds("TRANSACTION")
                val pendingCrInsertIds = syncQueueDao.getPendingEntityIds("CREDIT")
                val pendingPyInsertIds = syncQueueDao.getPendingEntityIds("PAYMENT")

                var localDataModified = false

                // Reconcile Transactions: only write if entity is new or modified
                val localTxs = transactionDao.getAll().associateBy { it.id }
                for (tx in remoteData.transactions) {
                    val local = localTxs[tx.id]
                    if (local == null || local != tx) {
                        transactionDao.insert(tx)
                        localDataModified = true
                    }
                }
                val remoteTxIds = remoteData.transactions.map { it.id }.toSet()
                for (localTx in localTxs.values) {
                    if (localTx.id !in remoteTxIds && localTx.id !in pendingTxInsertIds) {
                        transactionDao.deleteById(localTx.id)
                        localDataModified = true
                    }
                }

                // Reconcile Credits: only write if entity is new or modified
                val localCrs = creditDao.getAll().associateBy { it.id }
                for (cr in remoteData.credits) {
                    val local = localCrs[cr.id]
                    if (local == null || local != cr) {
                        creditDao.insert(cr)
                        localDataModified = true
                    }
                }
                val remoteCrIds = remoteData.credits.map { it.id }.toSet()
                for (localCr in localCrs.values) {
                    if (localCr.id !in remoteCrIds && localCr.id !in pendingCrInsertIds) {
                        creditDao.deleteById(localCr.id)
                        localDataModified = true
                    }
                }

                // Reconcile Payments: only write if entity is new or modified
                val localPys = paymentDao.getAll().associateBy { it.id }
                for (py in remoteData.payments) {
                    val local = localPys[py.id]
                    if (local == null || local != py) {
                        paymentDao.insert(py)
                        localDataModified = true
                    }
                }
                val remotePyIds = remoteData.payments.map { it.id }.toSet()
                for (localPy in localPys.values) {
                    if (localPy.id !in remotePyIds && localPy.id !in pendingPyInsertIds) {
                        paymentDao.deleteById(localPy.id)
                        localDataModified = true
                    }
                }

                if (localDataModified) {
                    refreshLocalData()
                }
            }

            // 4. Update complete DB health status
            val localDiag = LocalDbDiagnostic(
                databaseName = configProvider.dbName,
                version = configProvider.dbVersion,
                transactionCount = transactionDao.getCount(),
                creditCount = creditDao.getCount(),
                paymentCount = paymentDao.getCount(),
                isHealthy = true
            )
            _dbStatus.value = FullDatabaseStatus(
                local = localDiag,
                remote = remoteDiag.copy(
                    status = ConnectionStatus.CONNECTED,
                    message = "Sincronizado con Turso Cloud"
                )
            )
            lastSyncTimestamp = System.currentTimeMillis()
            return@withLock true
        }
    }

    suspend fun checkDatabaseHealth(): FullDatabaseStatus = withContext(Dispatchers.IO) {
        _dbStatus.value = _dbStatus.value.copy(
            remote = _dbStatus.value.remote.copy(status = ConnectionStatus.CONNECTING)
        )

        val txCount = transactionDao.getCount()
        val crCount = creditDao.getCount()
        val pyCount = paymentDao.getCount()

        val localDiag = LocalDbDiagnostic(
            databaseName = configProvider.dbName,
            version = configProvider.dbVersion,
            transactionCount = txCount,
            creditCount = crCount,
            paymentCount = pyCount,
            isHealthy = true
        )

        val remoteDiag = tursoSyncClient.testConnection()

        val fullStatus = FullDatabaseStatus(
            local = localDiag,
            remote = remoteDiag
        )
        _dbStatus.value = fullStatus
        return@withContext fullStatus
    }

    private fun updateLocalHealthStatus() {
        val txCount = transactionDao.getCount()
        val crCount = creditDao.getCount()
        val pyCount = paymentDao.getCount()

        val localDiag = LocalDbDiagnostic(
            databaseName = configProvider.dbName,
            version = configProvider.dbVersion,
            transactionCount = txCount,
            creditCount = crCount,
            paymentCount = pyCount,
            isHealthy = true
        )
        _dbStatus.value = _dbStatus.value.copy(local = localDiag)
    }

    suspend fun switchRemoteToken(): FullDatabaseStatus = withContext(Dispatchers.IO) {
        tursoSyncClient.switchToken()
        checkDatabaseHealth()
    }

    suspend fun syncRemoteSchema(): Boolean = withContext(Dispatchers.IO) {
        val ok = tursoSyncClient.initializeRemoteDatabase()
        checkDatabaseHealth()
        return@withContext ok
    }

    suspend fun clearAllData(): Boolean = withContext(Dispatchers.IO) {
        transactionDao.deleteAll()
        creditDao.deleteAll()
        paymentDao.deleteAll()
        syncQueueDao.clear()

        val ok1 = tursoSyncClient.executeQuery("DELETE FROM payments;")
        val ok2 = tursoSyncClient.executeQuery("DELETE FROM credits;")
        val ok3 = tursoSyncClient.executeQuery("DELETE FROM transactions;")

        refreshLocalData()
        checkDatabaseHealth()
        return@withContext ok1 && ok2 && ok3
    }

    private fun calculateSummary(txList: List<Transaction>, crList: List<Credit>) {
        var income = 0.0
        var expense = 0.0

        for (tx in txList) {
            if (tx.type == TransactionType.INCOME) {
                income += tx.amount
            } else {
                expense += tx.amount
            }
        }

        var debt = 0.0
        var activeCredits = 0
        for (cr in crList) {
            debt += cr.remainingAmount
            if (cr.remainingAmount > 0) {
                activeCredits++
            }
        }

        _summary.value = FinancialSummary(
            totalBalance = income - expense,
            totalIncome = income,
            totalExpense = expense,
            totalDebt = debt,
            activeCreditsCount = activeCredits
        )
    }

    suspend fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        dateString: String? = null
    ) = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val date = dateString ?: dateFormat.format(Date())
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            title = title,
            amount = amount,
            type = type,
            category = category,
            date = date
        )
        val typeString = if (type == TransactionType.INCOME) "income" else "expense"
        transactionDao.insert(TransactionEntity.fromDomain(transaction))

        val sql = "INSERT OR REPLACE INTO transactions (id, title, amount, type, category, date) VALUES ('${transaction.id}', '${title.replace("'", "''")}', $amount, '$typeString', '${category.replace("'", "''")}', '$date');"
        syncQueueDao.insert(
            SyncQueueEntity(
                entityType = "TRANSACTION",
                entityId = transaction.id,
                operation = "INSERT",
                sqlCommand = sql
            )
        )

        refreshLocalData()
        syncData()
    }

    suspend fun deleteTransaction(id: String) = withContext(Dispatchers.IO) {
        transactionDao.deleteById(id)
        val sql = "DELETE FROM transactions WHERE id = '$id';"
        syncQueueDao.insert(
            SyncQueueEntity(
                entityType = "TRANSACTION",
                entityId = id,
                operation = "DELETE",
                sqlCommand = sql
            )
        )

        refreshLocalData()
        syncData()
    }

    suspend fun addCredit(
        title: String,
        totalAmount: Double,
        dueDate: String
    ) = withContext(Dispatchers.IO) {
        val credit = Credit(
            id = UUID.randomUUID().toString(),
            title = title,
            totalAmount = totalAmount,
            remainingAmount = totalAmount,
            dueDate = dueDate
        )
        creditDao.insert(CreditEntity.fromDomain(credit))

        val sql = "INSERT OR REPLACE INTO credits (id, title, total_amount, remaining_amount, due_date) VALUES ('${credit.id}', '${title.replace("'", "''")}', $totalAmount, $totalAmount, '$dueDate');"
        syncQueueDao.insert(
            SyncQueueEntity(
                entityType = "CREDIT",
                entityId = credit.id,
                operation = "INSERT",
                sqlCommand = sql
            )
        )

        refreshLocalData()
        syncData()
    }

    suspend fun recordPayment(
        creditId: String,
        amount: Double,
        dateString: String? = null
    ) = withContext(Dispatchers.IO) {
        val currentCredits = _credits.value
        val credit = currentCredits.find { it.id == creditId } ?: return@withContext

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val date = dateString ?: dateFormat.format(Date())

        val payment = Payment(
            id = UUID.randomUUID().toString(),
            creditId = creditId,
            amount = amount,
            date = date
        )
        paymentDao.insert(PaymentEntity.fromDomain(payment))

        val newRemaining = (credit.remainingAmount - amount).coerceAtLeast(0.0)
        creditDao.updateRemainingAmount(creditId, newRemaining)

        val expenseTx = Transaction(
            id = UUID.randomUUID().toString(),
            title = "Abono: ${credit.title}",
            amount = amount,
            type = TransactionType.EXPENSE,
            category = "Pago Crédito",
            date = date
        )
        transactionDao.insert(TransactionEntity.fromDomain(expenseTx))

        val paySql = "INSERT OR REPLACE INTO payments (id, credit_id, amount, date) VALUES ('${payment.id}', '$creditId', $amount, '$date');"
        val updateCreditSql = "UPDATE credits SET remaining_amount = $newRemaining WHERE id = '$creditId';"
        val txSql = "INSERT OR REPLACE INTO transactions (id, title, amount, type, category, date) VALUES ('${expenseTx.id}', '${expenseTx.title.replace("'", "''")}', $amount, 'expense', 'Pago Crédito', '$date');"

        syncQueueDao.insert(SyncQueueEntity(entityType = "PAYMENT", entityId = payment.id, operation = "INSERT", sqlCommand = paySql))
        syncQueueDao.insert(SyncQueueEntity(entityType = "CREDIT", entityId = creditId, operation = "UPDATE", sqlCommand = updateCreditSql))
        syncQueueDao.insert(SyncQueueEntity(entityType = "TRANSACTION", entityId = expenseTx.id, operation = "INSERT", sqlCommand = txSql))

        refreshLocalData()
        syncData()
    }

    suspend fun deleteCredit(id: String) = withContext(Dispatchers.IO) {
        paymentDao.deleteByCreditId(id)
        creditDao.deleteById(id)

        val delPaySql = "DELETE FROM payments WHERE credit_id = '$id';"
        val sql = "DELETE FROM credits WHERE id = '$id';"

        syncQueueDao.insert(
            SyncQueueEntity(
                entityType = "PAYMENT",
                entityId = id,
                operation = "DELETE",
                sqlCommand = delPaySql
            )
        )
        syncQueueDao.insert(
            SyncQueueEntity(
                entityType = "CREDIT",
                entityId = id,
                operation = "DELETE",
                sqlCommand = sql
            )
        )

        refreshLocalData()
        syncData()
    }

    private fun purgeLegacySampleData() {
        if (hasPurgedLegacySampleData) return
        hasPurgedLegacySampleData = true

        val sampleTitles = setOf(
            "Salario Mensual", "Supermercado Exito", "Pago Arriendo",
            "Freelance Diseño", "Servicios Públicos"
        )
        for (tx in transactionDao.getAll()) {
            if (tx.title in sampleTitles) {
                transactionDao.deleteById(tx.id)
            }
        }

        val sampleCreditTitles = setOf(
            "Tarjeta de Crédito Visa", "Préstamo Libre Inversión"
        )
        for (cr in creditDao.getAll()) {
            if (cr.title in sampleCreditTitles) {
                creditDao.deleteById(cr.id)
            }
        }
    }
}
