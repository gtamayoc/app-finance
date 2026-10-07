package com.gtc.app_finance.cloud.sync

import android.util.Log
import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.DatabaseDocument
import com.gtc.app_finance.cloud.domain.ports.IDatabaseService
import com.gtc.app_finance.cloud.sync.conflict.ConflictDecision
import com.gtc.app_finance.cloud.sync.conflict.ConflictResolver
import com.gtc.app_finance.cloud.sync.mapper.CreditDocumentMapper
import com.gtc.app_finance.cloud.sync.mapper.PaymentDocumentMapper
import com.gtc.app_finance.cloud.sync.mapper.TransactionDocumentMapper
import com.gtc.app_finance.cloud.sync.mutation.CloudMutation
import com.gtc.app_finance.cloud.sync.mutation.ISyncMutationQueue
import com.gtc.app_finance.cloud.sync.mutation.MutationType
import com.gtc.app_finance.data.dao.CreditDao
import com.gtc.app_finance.data.dao.PaymentDao
import com.gtc.app_finance.data.dao.TransactionDao
import com.gtc.app_finance.data.entity.CreditEntity
import com.gtc.app_finance.data.entity.PaymentEntity
import com.gtc.app_finance.data.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class CloudSyncEngine(
    private val databaseService: IDatabaseService,
    private val mutationQueue: ISyncMutationQueue,
    private val transactionDao: TransactionDao,
    private val creditDao: CreditDao,
    private val paymentDao: PaymentDao,
    private val conflictResolver: ConflictResolver = ConflictResolver(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val syncMutex = Mutex()
    private var liveSyncJob: Job? = null

    suspend fun queueTransactionUpsert(entity: TransactionEntity) {
        val doc = TransactionDocumentMapper.toDocument(entity)
        mutationQueue.enqueue(
            CloudMutation(
                collection = TransactionDocumentMapper.COLLECTION_NAME,
                entityId = entity.id,
                type = MutationType.UPSERT,
                payload = doc.data
            )
        )
    }

    suspend fun queueTransactionDelete(id: String) {
        mutationQueue.enqueue(
            CloudMutation(
                collection = TransactionDocumentMapper.COLLECTION_NAME,
                entityId = id,
                type = MutationType.DELETE
            )
        )
    }

    suspend fun queueCreditUpsert(entity: CreditEntity) {
        val doc = CreditDocumentMapper.toDocument(entity)
        mutationQueue.enqueue(
            CloudMutation(
                collection = CreditDocumentMapper.COLLECTION_NAME,
                entityId = entity.id,
                type = MutationType.UPSERT,
                payload = doc.data
            )
        )
    }

    suspend fun queueCreditDelete(id: String) {
        mutationQueue.enqueue(
            CloudMutation(
                collection = CreditDocumentMapper.COLLECTION_NAME,
                entityId = id,
                type = MutationType.DELETE
            )
        )
    }

    suspend fun queuePaymentUpsert(entity: PaymentEntity) {
        val doc = PaymentDocumentMapper.toDocument(entity)
        mutationQueue.enqueue(
            CloudMutation(
                collection = PaymentDocumentMapper.COLLECTION_NAME,
                entityId = entity.id,
                type = MutationType.UPSERT,
                payload = doc.data
            )
        )
    }

    suspend fun queuePaymentDelete(id: String) {
        mutationQueue.enqueue(
            CloudMutation(
                collection = PaymentDocumentMapper.COLLECTION_NAME,
                entityId = id,
                type = MutationType.DELETE
            )
        )
    }

    suspend fun pushPendingMutations(): CloudResult<Int> = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val pending = mutationQueue.getPending()
            var processed = 0

            for (mutation in pending) {
                val result: CloudResult<Unit> = when (mutation.type) {
                    MutationType.UPSERT -> {
                        databaseService.saveDocument(
                            collection = mutation.collection,
                            id = mutation.entityId,
                            data = mutation.payload ?: emptyMap()
                        )
                    }
                    MutationType.DELETE -> {
                        databaseService.deleteDocument(
                            collection = mutation.collection,
                            id = mutation.entityId
                        )
                    }
                }

                val isIdempotentDelete = mutation.type == MutationType.DELETE && result.errorOrNull() is CloudError.DatabaseError.DocumentNotFound
                if (result.isSuccess || isIdempotentDelete) {
                    mutationQueue.remove(mutation.id)
                    processed++
                } else {
                    Log.w("CloudSyncEngine", "Failed pushing mutation ${mutation.id}: ${result.errorOrNull()?.message}")
                    if (result.errorOrNull() is CloudError.NetworkError) {
                        return@withLock CloudResult.Failure(result.errorOrNull()!!)
                    }
                }
            }
            CloudResult.Success(processed)
        }
    }

    fun startLiveSync(): Job {
        liveSyncJob?.cancel()
        val job = scope.launch {
            launch {
                databaseService.streamDocuments(TransactionDocumentMapper.COLLECTION_NAME).collect { result ->
                    if (result is CloudResult.Success) {
                        reconcileRemoteTransactions(result.data)
                    }
                }
            }
            launch {
                databaseService.streamDocuments(CreditDocumentMapper.COLLECTION_NAME).collect { result ->
                    if (result is CloudResult.Success) {
                        reconcileRemoteCredits(result.data)
                    }
                }
            }
            launch {
                databaseService.streamDocuments(PaymentDocumentMapper.COLLECTION_NAME).collect { result ->
                    if (result is CloudResult.Success) {
                        reconcileRemotePayments(result.data)
                    }
                }
            }
        }
        liveSyncJob = job
        return job
    }

    suspend fun reconcileRemoteTransactions(remoteDocs: List<DatabaseDocument>) = withContext(Dispatchers.IO) {
        val pendingMutationIds = mutationQueue.getPending()
            .filter { it.collection == TransactionDocumentMapper.COLLECTION_NAME }
            .map { it.entityId }
            .toSet()

        val localEntities = transactionDao.getAll().associateBy { it.id }

        val remoteIds = remoteDocs.map { it.id }.toSet()

        for (doc in remoteDocs) {
            val hasPending = doc.id in pendingMutationIds
            val local = localEntities[doc.id]
            val localTimestamp: Long? = null

            val decision = conflictResolver.shouldApplyRemote(doc, localTimestamp, hasPending)
            if (decision == ConflictDecision.APPLY_REMOTE) {
                val entity = TransactionDocumentMapper.toEntity(doc)
                if (entity != null && (local == null || local != entity)) {
                    transactionDao.insert(entity)
                }
            }
        }

        for (local in localEntities.values) {
            if (local.id !in remoteIds && local.id !in pendingMutationIds) {
                transactionDao.deleteById(local.id)
            }
        }
    }

    suspend fun reconcileRemoteCredits(remoteDocs: List<DatabaseDocument>) = withContext(Dispatchers.IO) {
        val pendingMutationIds = mutationQueue.getPending()
            .filter { it.collection == CreditDocumentMapper.COLLECTION_NAME }
            .map { it.entityId }
            .toSet()

        val localEntities = creditDao.getAll().associateBy { it.id }
        val remoteIds = remoteDocs.map { it.id }.toSet()

        for (doc in remoteDocs) {
            val hasPending = doc.id in pendingMutationIds
            val local = localEntities[doc.id]
            val localTimestamp: Long? = null

            val decision = conflictResolver.shouldApplyRemote(doc, localTimestamp, hasPending)
            if (decision == ConflictDecision.APPLY_REMOTE) {
                val entity = CreditDocumentMapper.toEntity(doc)
                if (entity != null && (local == null || local != entity)) {
                    creditDao.insert(entity)
                }
            }
        }

        for (local in localEntities.values) {
            if (local.id !in remoteIds && local.id !in pendingMutationIds) {
                paymentDao.deleteByCreditId(local.id)
                creditDao.deleteById(local.id)
            }
        }
    }

    suspend fun reconcileRemotePayments(remoteDocs: List<DatabaseDocument>) = withContext(Dispatchers.IO) {
        val pendingMutationIds = mutationQueue.getPending()
            .filter { it.collection == PaymentDocumentMapper.COLLECTION_NAME }
            .map { it.entityId }
            .toSet()

        val validCreditIds = creditDao.getAllIds()
        val localEntities = paymentDao.getAll().associateBy { it.id }
        val remoteIds = remoteDocs.map { it.id }.toSet()

        for (doc in remoteDocs) {
            if (!conflictResolver.isReferentiallyValid(PaymentDocumentMapper.COLLECTION_NAME, doc, validCreditIds)) {
                Log.w("CloudSyncEngine", "Skipping orphan payment document ${doc.id}")
                continue
            }

            val hasPending = doc.id in pendingMutationIds
            val local = localEntities[doc.id]
            val localTimestamp: Long? = null

            val decision = conflictResolver.shouldApplyRemote(doc, localTimestamp, hasPending)
            if (decision == ConflictDecision.APPLY_REMOTE) {
                val entity = PaymentDocumentMapper.toEntity(doc)
                if (entity != null && (local == null || local != entity)) {
                    paymentDao.insert(entity)
                }
            }
        }

        for (local in localEntities.values) {
            if (local.id !in remoteIds && local.id !in pendingMutationIds) {
                paymentDao.deleteById(local.id)
            }
        }
    }

    suspend fun syncAll(): CloudResult<Unit> = withContext(Dispatchers.IO) {
        pushPendingMutations()
        val txQuery = databaseService.queryDocuments(TransactionDocumentMapper.COLLECTION_NAME)
        if (txQuery is CloudResult.Success) reconcileRemoteTransactions(txQuery.data)

        val crQuery = databaseService.queryDocuments(CreditDocumentMapper.COLLECTION_NAME)
        if (crQuery is CloudResult.Success) reconcileRemoteCredits(crQuery.data)

        val pyQuery = databaseService.queryDocuments(PaymentDocumentMapper.COLLECTION_NAME)
        if (pyQuery is CloudResult.Success) reconcileRemotePayments(pyQuery.data)

        CloudResult.Success(Unit)
    }

    fun stopLiveSync() {
        liveSyncJob?.cancel()
        liveSyncJob = null
    }
}
