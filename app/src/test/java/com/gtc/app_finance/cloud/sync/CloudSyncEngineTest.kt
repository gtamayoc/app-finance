package com.gtc.app_finance.cloud.sync

import com.gtc.app_finance.cloud.domain.model.DatabaseDocument
import com.gtc.app_finance.cloud.infrastructure.memory.InMemoryDatabaseAdapter
import com.gtc.app_finance.cloud.sync.conflict.ConflictResolver
import com.gtc.app_finance.cloud.sync.mapper.TransactionDocumentMapper
import com.gtc.app_finance.cloud.sync.mutation.CloudMutation
import com.gtc.app_finance.cloud.sync.mutation.LocalSyncMutationQueue
import com.gtc.app_finance.cloud.sync.mutation.MutationType
import com.gtc.app_finance.data.dao.CreditDao
import com.gtc.app_finance.data.dao.PaymentDao
import com.gtc.app_finance.data.dao.TransactionDao
import com.gtc.app_finance.data.entity.CreditEntity
import com.gtc.app_finance.data.entity.PaymentEntity
import com.gtc.app_finance.data.entity.TransactionEntity
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CloudSyncEngineTest {

    private lateinit var databaseService: InMemoryDatabaseAdapter
    private lateinit var mutationQueue: LocalSyncMutationQueue
    private val transactionDao: TransactionDao = mockk(relaxed = true)
    private val creditDao: CreditDao = mockk(relaxed = true)
    private val paymentDao: PaymentDao = mockk(relaxed = true)
    private val conflictResolver = ConflictResolver()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var engine: CloudSyncEngine

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0

        databaseService = InMemoryDatabaseAdapter()
        mutationQueue = LocalSyncMutationQueue()

        every { transactionDao.getAll() } returns emptyList()
        every { creditDao.getAll() } returns emptyList()
        every { creditDao.getAllIds() } returns emptySet()
        every { paymentDao.getAll() } returns emptyList()

        engine = CloudSyncEngine(
            databaseService = databaseService,
            mutationQueue = mutationQueue,
            transactionDao = transactionDao,
            creditDao = creditDao,
            paymentDao = paymentDao,
            conflictResolver = conflictResolver,
            scope = testScope
        )
    }

    @Test
    fun `queueing upsert methods add proper mutations to queue`() = runTest {
        val txEntity = TransactionEntity(
            id = "tx-1",
            title = "Test TX",
            amount = 100.0,
            type = "expense",
            category = "General",
            date = "2026-10-07"
        )
        engine.queueTransactionUpsert(txEntity)
        assertEquals(1, mutationQueue.getCount())
        var pending = mutationQueue.getPending()[0]
        assertEquals("transactions", pending.collection)
        assertEquals("tx-1", pending.entityId)
        assertEquals(MutationType.UPSERT, pending.type)

        val creditEntity = CreditEntity(
            id = "cred-1",
            title = "Car Loan",
            totalAmount = 5000.0,
            remainingAmount = 4000.0,
            dueDate = "2027-01-01"
        )
        engine.queueCreditUpsert(creditEntity)
        assertEquals(2, mutationQueue.getCount())

        val payEntity = PaymentEntity(
            id = "pay-1",
            creditId = "cred-1",
            amount = 200.0,
            date = "2026-10-07"
        )
        engine.queuePaymentUpsert(payEntity)
        assertEquals(3, mutationQueue.getCount())
    }

    @Test
    fun `queueing delete methods enqueue DELETE mutations`() = runTest {
        engine.queueTransactionDelete("tx-del-1")
        engine.queueCreditDelete("cred-del-1")
        engine.queuePaymentDelete("pay-del-1")

        assertEquals(3, mutationQueue.getCount())
        val pending = mutationQueue.getPending()
        assertTrue(pending.all { it.type == MutationType.DELETE })
    }

    @Test
    fun `pushPendingMutations executes mutations against remote database and clears queue`() = runTest {
        val txEntity = TransactionEntity(
            id = "tx-push-1",
            title = "Almuerzo",
            amount = 35.0,
            type = "expense",
            category = "Alimentación",
            date = "2026-10-07"
        )
        engine.queueTransactionUpsert(txEntity)
        engine.queueCreditDelete("cred-old-1")

        val result = engine.pushPendingMutations()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
        assertEquals(0, mutationQueue.getCount())

        // Verify document is in remote database
        val remoteDoc = databaseService.getDocument("transactions", "tx-push-1")
        assertTrue(remoteDoc.isSuccess)
        assertEquals("Almuerzo", remoteDoc.getOrNull()?.data?.get("title"))
    }

    @Test
    fun `reconcileRemoteTransactions inserts new remote transaction when no pending local edits`() = runTest {
        val remoteDoc = DatabaseDocument(
            id = "tx-remote-1",
            collection = "transactions",
            data = mapOf(
                "title" to "Freelance",
                "amount" to 500.0,
                "type" to "income",
                "category" to "Trabajo",
                "date" to "2026-10-07 10:00"
            ),
            updatedAtEpochMs = 1000L
        )

        engine.reconcileRemoteTransactions(listOf(remoteDoc))

        verify(exactly = 1) {
            transactionDao.insert(
                match { it.id == "tx-remote-1" && it.title == "Freelance" && it.amount == 500.0 }
            )
        }
    }

    @Test
    fun `reconcileRemoteTransactions ignores remote document when pending local mutation exists`() = runTest {
        mutationQueue.enqueue(
            CloudMutation(
                collection = "transactions",
                entityId = "tx-conflict-1",
                type = MutationType.UPSERT,
                payload = mapOf("title" to "Local Version")
            )
        )

        val remoteDoc = DatabaseDocument(
            id = "tx-conflict-1",
            collection = "transactions",
            data = mapOf(
                "title" to "Remote Version",
                "amount" to 300.0,
                "type" to "expense",
                "category" to "Test",
                "date" to "2026-10-07"
            ),
            updatedAtEpochMs = 1000L
        )

        engine.reconcileRemoteTransactions(listOf(remoteDoc))

        verify(exactly = 0) {
            transactionDao.insert(match { it.id == "tx-conflict-1" })
        }
    }

    @Test
    fun `reconcileRemoteTransactions deletes local transaction when missing remotely and not pending`() = runTest {
        val existingLocal = TransactionEntity(
            id = "tx-to-delete",
            title = "Old Local Tx",
            amount = 20.0,
            type = "expense",
            category = "Misc",
            date = "2026-10-01"
        )
        every { transactionDao.getAll() } returns listOf(existingLocal)

        // Remote list is empty
        engine.reconcileRemoteTransactions(emptyList())

        verify(exactly = 1) {
            transactionDao.deleteById("tx-to-delete")
        }
    }

    @Test
    fun `reconcileRemotePayments drops orphan payment when parent credit is absent locally`() = runTest {
        every { creditDao.getAllIds() } returns setOf("cred-existing")

        val orphanPaymentDoc = DatabaseDocument(
            id = "pay-orphan",
            collection = "payments",
            data = mapOf(
                "creditId" to "cred-non-existent",
                "amount" to 50.0,
                "date" to "2026-10-07"
            )
        )

        engine.reconcileRemotePayments(listOf(orphanPaymentDoc))

        verify(exactly = 0) {
            paymentDao.insert(any())
        }
    }

    @Test
    fun `reconcileRemotePayments inserts valid payment when parent credit exists locally`() = runTest {
        every { creditDao.getAllIds() } returns setOf("cred-existing")

        val validPaymentDoc = DatabaseDocument(
            id = "pay-valid",
            collection = "payments",
            data = mapOf(
                "creditId" to "cred-existing",
                "amount" to 50.0,
                "date" to "2026-10-07"
            )
        )

        engine.reconcileRemotePayments(listOf(validPaymentDoc))

        verify(exactly = 1) {
            paymentDao.insert(match { it.id == "pay-valid" && it.creditId == "cred-existing" })
        }
    }

    @Test
    fun `syncAll executes push and pull queries cleanly`() = runTest {
        // Enqueue a pending mutation
        engine.queueTransactionUpsert(
            TransactionEntity(
                id = "tx-sync-1",
                title = "Sync Test",
                amount = 120.0,
                type = "income",
                category = "General",
                date = "2026-10-07"
            )
        )

        val syncResult = engine.syncAll()

        assertTrue(syncResult.isSuccess)
        assertEquals(0, mutationQueue.getCount())
    }
}
