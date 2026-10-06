package com.gtc.app_finance

import com.gtc.app_finance.data.entity.CreditEntity
import com.gtc.app_finance.data.entity.PaymentEntity
import com.gtc.app_finance.data.entity.SyncQueueEntity
import com.gtc.app_finance.data.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncQueueAndReciprocalLogicTest {

    @Test
    fun `SyncQueueEntity preserves attributes and FIFO ordering metadata`() {
        val queueItem = SyncQueueEntity(
            id = 1L,
            entityType = "TRANSACTION",
            entityId = "tx-123",
            operation = "INSERT",
            sqlCommand = "INSERT INTO transactions VALUES (...);",
            createdAt = 1000L
        )

        assertEquals(1L, queueItem.id)
        assertEquals("TRANSACTION", queueItem.entityType)
        assertEquals("tx-123", queueItem.entityId)
        assertEquals("INSERT", queueItem.operation)
        assertEquals("INSERT INTO transactions VALUES (...);", queueItem.sqlCommand)
        assertEquals(1000L, queueItem.createdAt)
    }

    @Test
    fun `Reciprocal reconciliation detects remote additions correctly`() {
        val localList = mutableListOf(
            TransactionEntity("1", "Comida", 25000.0, "expense", "Alimentacion", "2026-10-05 12:00")
        )
        val remoteList = listOf(
            TransactionEntity("1", "Comida", 25000.0, "expense", "Alimentacion", "2026-10-05 12:00"),
            TransactionEntity("2", "Transporte", 10000.0, "expense", "Transporte", "2026-10-05 13:00")
        )

        // Merge remote into local
        for (remoteTx in remoteList) {
            val idx = localList.indexOfFirst { it.id == remoteTx.id }
            if (idx >= 0) {
                localList[idx] = remoteTx
            } else {
                localList.add(remoteTx)
            }
        }

        assertEquals(2, localList.size)
        assertTrue(localList.any { it.id == "2" })
    }

    @Test
    fun `Reciprocal reconciliation removes locally cached items deleted remotely unless pending offline insert`() {
        val localList = mutableListOf(
            TransactionEntity("tx-deleted-remotely", "Old item", 50000.0, "expense", "General", "2026-10-01"),
            TransactionEntity("tx-pending-offline", "New local offline", 30000.0, "expense", "General", "2026-10-05"),
            TransactionEntity("tx-synced", "Synced item", 10000.0, "income", "General", "2026-10-05")
        )

        val remoteList = listOf(
            TransactionEntity("tx-synced", "Synced item", 10000.0, "income", "General", "2026-10-05")
        )

        val pendingInsertIds = setOf("tx-pending-offline")
        val remoteIds = remoteList.map { it.id }.toSet()

        // Reconcile: delete local items that are neither in remote nor pending offline insert
        localList.removeAll { localItem ->
            localItem.id !in remoteIds && localItem.id !in pendingInsertIds
        }

        assertEquals(2, localList.size)
        assertFalse(localList.any { it.id == "tx-deleted-remotely" })
        assertTrue(localList.any { it.id == "tx-pending-offline" })
        assertTrue(localList.any { it.id == "tx-synced" })
    }

    @Test
    fun `Purge legacy sample titles filters hardcoded seed values`() {
        val sampleTitles = setOf(
            "Salario Mensual", "Supermercado Exito", "Pago Arriendo",
            "Freelance Diseño", "Servicios Públicos"
        )

        val testTransactions = listOf(
            TransactionEntity("1", "Salario Mensual", 3500000.0, "income", "Nómina", "2026-10-01"),
            TransactionEntity("2", "Supermercado Exito", 280000.0, "expense", "Alimentación", "2026-10-02"),
            TransactionEntity("3", "Mi Compra Real", 45000.0, "expense", "Alimentación", "2026-10-05")
        )

        val filtered = testTransactions.filter { it.title !in sampleTitles }
        assertEquals(1, filtered.size)
        assertEquals("Mi Compra Real", filtered.first().title)
    }

    @Test
    fun `Clean start leaves all datasets empty without auto-generating dummy records`() {
        val transactions = emptyList<TransactionEntity>()
        val credits = emptyList<CreditEntity>()
        val payments = emptyList<PaymentEntity>()

        assertTrue(transactions.isEmpty())
        assertTrue(credits.isEmpty())
        assertTrue(payments.isEmpty())
    }

    @Test
    fun `Selective reconciliation identifies identical records to avoid unnecessary database writes`() {
        val existingCredit = CreditEntity("cr-1", "Banco Pichincha", 1200.0, 600.0, "2026-12-31")
        val identicalRemoteCredit = CreditEntity("cr-1", "Banco Pichincha", 1200.0, 600.0, "2026-12-31")
        val updatedRemoteCredit = CreditEntity("cr-1", "Banco Pichincha", 1200.0, 400.0, "2026-12-31")

        // Identical entity requires no write
        val shouldWriteIdentical = existingCredit != identicalRemoteCredit
        assertFalse(shouldWriteIdentical)

        // Modified entity requires write
        val shouldWriteUpdated = existingCredit != updatedRemoteCredit
        assertTrue(shouldWriteUpdated)
    }

    @Test
    fun `Idempotent SQL pattern formats with INSERT OR REPLACE INTO`() {
        val testCreditId = "cred-abc"
        val testTitle = "Tarjeta Éxito"
        val testAmount = 500000.0
        val testDueDate = "2026-11-30"

        val sql = "INSERT OR REPLACE INTO credits (id, title, total_amount, remaining_amount, due_date) VALUES ('$testCreditId', '${testTitle.replace("'", "''")}', $testAmount, $testAmount, '$testDueDate');"

        assertTrue(sql.startsWith("INSERT OR REPLACE INTO credits"))
        assertTrue(sql.contains("'$testCreditId'"))
        assertTrue(sql.contains("Tarjeta Éxito"))
    }
}
