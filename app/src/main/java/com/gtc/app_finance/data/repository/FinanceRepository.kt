package com.gtc.app_finance.data.repository

import com.gtc.app_finance.data.dao.CreditDao
import com.gtc.app_finance.data.dao.PaymentDao
import com.gtc.app_finance.data.dao.TransactionDao
import com.gtc.app_finance.data.database.TursoConfigProvider
import com.gtc.app_finance.data.database.TursoSyncClient
import com.gtc.app_finance.data.entity.CreditEntity
import com.gtc.app_finance.data.entity.PaymentEntity
import com.gtc.app_finance.data.entity.TransactionEntity
import com.gtc.app_finance.domain.model.ConnectionStatus
import com.gtc.app_finance.domain.model.Credit
import com.gtc.app_finance.domain.model.FinancialSummary
import com.gtc.app_finance.domain.model.FullDatabaseStatus
import com.gtc.app_finance.domain.model.LocalDbDiagnostic
import com.gtc.app_finance.domain.model.Payment
import com.gtc.app_finance.domain.model.Transaction
import com.gtc.app_finance.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val creditDao: CreditDao,
    private val paymentDao: PaymentDao,
    private val tursoSyncClient: TursoSyncClient,
    private val configProvider: TursoConfigProvider
) {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _credits = MutableStateFlow<List<Credit>>(emptyList())
    val credits: StateFlow<List<Credit>> = _credits.asStateFlow()

    private val _summary = MutableStateFlow(FinancialSummary())
    val summary: StateFlow<FinancialSummary> = _summary.asStateFlow()

    private val _dbStatus = MutableStateFlow(FullDatabaseStatus())
    val dbStatus: StateFlow<FullDatabaseStatus> = _dbStatus.asStateFlow()

    suspend fun refreshData(): Unit = withContext(Dispatchers.IO) {
        val txEntities = transactionDao.getAll()
        val crEntities = creditDao.getAll()

        // Seed initial mock data if database is completely fresh
        if (txEntities.isEmpty() && crEntities.isEmpty()) {
            seedSampleData()
            refreshData()
            return@withContext
        }

        val txList = txEntities.map { it.toDomain() }
        val crList = crEntities.map { it.toDomain() }

        _transactions.value = txList
        _credits.value = crList

        calculateSummary(txList, crList)

        // Run health check and auto-sync schema
        checkDatabaseHealth()
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

    suspend fun switchRemoteToken(): FullDatabaseStatus = withContext(Dispatchers.IO) {
        tursoSyncClient.switchToken()
        checkDatabaseHealth()
    }

    suspend fun syncRemoteSchema(): Boolean = withContext(Dispatchers.IO) {
        val ok = tursoSyncClient.initializeRemoteDatabase()
        checkDatabaseHealth()
        return@withContext ok
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

        // Remote sync SQL to Turso Cloud
        val sql = "INSERT INTO transactions (id, title, amount, type, category, date) VALUES ('${transaction.id}', '${title.replace("'", "''")}', $amount, '$typeString', '${category.replace("'", "''")}', '$date');"
        tursoSyncClient.executeQuery(sql)

        refreshData()
    }

    suspend fun deleteTransaction(id: String) = withContext(Dispatchers.IO) {
        transactionDao.deleteById(id)
        val sql = "DELETE FROM transactions WHERE id = '$id';"
        tursoSyncClient.executeQuery(sql)
        refreshData()
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

        val sql = "INSERT INTO credits (id, title, total_amount, remaining_amount, due_date) VALUES ('${credit.id}', '${title.replace("'", "''")}', $totalAmount, $totalAmount, '$dueDate');"
        tursoSyncClient.executeQuery(sql)

        refreshData()
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

        // Remote sync SQL to Turso Cloud
        val paySql = "INSERT INTO payments (id, credit_id, amount, date) VALUES ('${payment.id}', '$creditId', $amount, '$date');"
        val updateCreditSql = "UPDATE credits SET remaining_amount = $newRemaining WHERE id = '$creditId';"
        tursoSyncClient.executeQuery(paySql)
        tursoSyncClient.executeQuery(updateCreditSql)

        refreshData()
    }

    suspend fun deleteCredit(id: String) = withContext(Dispatchers.IO) {
        creditDao.deleteById(id)
        val sql = "DELETE FROM credits WHERE id = '$id';"
        tursoSyncClient.executeQuery(sql)
        refreshData()
    }

    private fun seedSampleData() {
        val sampleTxs = listOf(
            TransactionEntity(UUID.randomUUID().toString(), "Salario Mensual", 3500000.0, "income", "Nómina", "2026-10-01 09:00"),
            TransactionEntity(UUID.randomUUID().toString(), "Supermercado Exito", 280000.0, "expense", "Alimentación", "2026-10-02 14:30"),
            TransactionEntity(UUID.randomUUID().toString(), "Pago Arriendo", 1200000.0, "expense", "Vivienda", "2026-10-03 10:15"),
            TransactionEntity(UUID.randomUUID().toString(), "Freelance Diseño", 650000.0, "income", "Trabajos", "2026-10-04 16:45"),
            TransactionEntity(UUID.randomUUID().toString(), "Servicios Públicos", 185000.0, "expense", "Servicios", "2026-10-05 11:20")
        )
        for (tx in sampleTxs) {
            transactionDao.insert(tx)
        }

        val sampleCredits = listOf(
            CreditEntity(UUID.randomUUID().toString(), "Tarjeta de Crédito Visa", 2000000.0, 1200000.0, "2026-10-25"),
            CreditEntity(UUID.randomUUID().toString(), "Préstamo Libre Inversión", 5000000.0, 3100000.0, "2026-11-10")
        )
        for (cr in sampleCredits) {
            creditDao.insert(cr)
        }
    }
}
