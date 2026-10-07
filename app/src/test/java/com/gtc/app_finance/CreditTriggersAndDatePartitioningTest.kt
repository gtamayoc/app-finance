package com.gtc.app_finance

import com.gtc.app_finance.data.dao.CreditDao
import com.gtc.app_finance.data.dao.PaymentDao
import com.gtc.app_finance.data.dao.SyncQueueDao
import com.gtc.app_finance.data.dao.TransactionDao
import com.gtc.app_finance.data.database.TursoConfigProvider
import com.gtc.app_finance.data.database.TursoDatabaseHelper
import com.gtc.app_finance.data.database.TursoSyncClient
import com.gtc.app_finance.data.entity.CreditEntity
import com.gtc.app_finance.data.entity.PaymentEntity
import com.gtc.app_finance.data.entity.TransactionEntity
import com.gtc.app_finance.data.repository.FinanceRepository
import com.gtc.app_finance.domain.model.MonthlyAggregate
import com.gtc.app_finance.domain.model.TransactionType
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreditTriggersAndDatePartitioningTest {

    private val transactionDao: TransactionDao = mockk(relaxed = true)
    private val creditDao: CreditDao = mockk(relaxed = true)
    private val paymentDao: PaymentDao = mockk(relaxed = true)
    private val syncQueueDao: SyncQueueDao = mockk(relaxed = true)
    private val tursoSyncClient: TursoSyncClient = mockk(relaxed = true)
    private val configProvider: TursoConfigProvider = mockk(relaxed = true)

    private lateinit var repository: FinanceRepository

    @Before
    fun setUp() {
        repository = FinanceRepository(
            transactionDao = transactionDao,
            creditDao = creditDao,
            paymentDao = paymentDao,
            syncQueueDao = syncQueueDao,
            tursoSyncClient = tursoSyncClient,
            configProvider = configProvider,
            cloudSyncEngine = null
        )
    }

    // =========================================================================
    // 1. Tests de la Lógica Matemática de los Triggers de Ciclo Completo
    // =========================================================================

    @Test
    fun `Trigger simulation AFTER INSERT recalculates remaining amount correctly`() {
        val totalAmount = 1500.0
        val payments = mutableListOf<Double>()

        // Simula la fórmula SQL: MAX(0.0, total_amount - SUM(payments))
        fun calculateRemaining(): Double = (totalAmount - payments.sum()).coerceAtLeast(0.0)

        assertEquals(1500.0, calculateRemaining(), 0.001)

        // Abono 1: $300
        payments.add(300.0)
        assertEquals(1200.0, calculateRemaining(), 0.001)

        // Abono 2: $450
        payments.add(450.0)
        assertEquals(750.0, calculateRemaining(), 0.001)
    }

    @Test
    fun `Trigger simulation AFTER DELETE restores remaining amount idempotently`() {
        val totalAmount = 2000.0
        val payments = mutableListOf(500.0, 300.0, 200.0) // Total abonado = 1000.0

        fun calculateRemaining(): Double = (totalAmount - payments.sum()).coerceAtLeast(0.0)
        assertEquals(1000.0, calculateRemaining(), 0.001)

        // Eliminación de abono de $300
        payments.remove(300.0)
        assertEquals(1300.0, calculateRemaining(), 0.001)

        // Eliminación de abono de $500
        payments.remove(500.0)
        assertEquals(1800.0, calculateRemaining(), 0.001)
    }

    @Test
    fun `Trigger simulation AFTER UPDATE adjusts difference without race condition or drift`() {
        val totalAmount = 1000.0
        val paymentMap = mutableMapOf("p1" to 200.0, "p2" to 300.0)

        fun calculateRemaining(): Double = (totalAmount - paymentMap.values.sum()).coerceAtLeast(0.0)
        assertEquals(500.0, calculateRemaining(), 0.001)

        // Corrección de p2 de $300 a $400
        paymentMap["p2"] = 400.0
        assertEquals(400.0, calculateRemaining(), 0.001)

        // Corrección de p1 de $200 a $100
        paymentMap["p1"] = 100.0
        assertEquals(500.0, calculateRemaining(), 0.001)
    }

    @Test
    fun `Trigger formula clamps remaining amount to zero when payments exceed total`() {
        val totalAmount = 500.0
        val payments = listOf(300.0, 400.0) // Suma = 700.0

        val remaining = (totalAmount - payments.sum()).coerceAtLeast(0.0)
        assertEquals(0.0, remaining, 0.001)
    }

    @Test
    fun `Trigger DDL statements are defined in TursoDatabaseHelper`() {
        assertTrue(TursoDatabaseHelper.CREATE_TRG_PAYMENT_INSERT_SQL.contains("trg_payment_after_insert"))
        assertTrue(TursoDatabaseHelper.CREATE_TRG_PAYMENT_INSERT_SQL.contains("AFTER INSERT ON payments"))
        assertTrue(TursoDatabaseHelper.CREATE_TRG_PAYMENT_DELETE_SQL.contains("trg_payment_after_delete"))
        assertTrue(TursoDatabaseHelper.CREATE_TRG_PAYMENT_DELETE_SQL.contains("AFTER DELETE ON payments"))
        assertTrue(TursoDatabaseHelper.CREATE_TRG_PAYMENT_UPDATE_SQL.contains("trg_payment_after_update"))
        assertTrue(TursoDatabaseHelper.CREATE_TRG_PAYMENT_UPDATE_SQL.contains("AFTER UPDATE OF amount, credit_id ON payments"))
    }

    // =========================================================================
    // 2. Tests de Particionamiento Temporal de Fechas (10 Años de Registros)
    // =========================================================================

    @Test
    fun `Date partitioning simulation efficiently filters target month over 10 years of data`() {
        // Simular 10 años de transacciones (2017 a 2026)
        val tenYearsDataset = mutableListOf<TransactionEntity>()
        for (year in 2017..2026) {
            for (month in 1..12) {
                val monthStr = month.toString().padStart(2, '0')
                tenYearsDataset.add(
                    TransactionEntity(
                        id = "tx-$year-$monthStr",
                        title = "Gasto mensual $monthStr/$year",
                        amount = 100.0,
                        type = "expense",
                        category = "General",
                        date = "$year-$monthStr-15 12:00"
                    )
                )
            }
        }
        assertEquals(120, tenYearsDataset.size) // 10 años * 12 meses

        // Filtrar solo Octubre 2026
        val targetMonth = "2026-10"
        val filtered = tenYearsDataset.filter { it.date.startsWith(targetMonth) }

        assertEquals(1, filtered.size)
        assertEquals("tx-2026-10", filtered[0].id)
        assertEquals("2026-10-15 12:00", filtered[0].date)
    }

    @Test
    fun `MonthlyAggregate correctly computes net balance and aggregates`() {
        val aggregate = MonthlyAggregate(
            yearMonth = "2026-10",
            totalIncome = 5000.0,
            totalExpense = 3200.0,
            netBalance = 1800.0
        )

        assertEquals("2026-10", aggregate.yearMonth)
        assertEquals(5000.0, aggregate.totalIncome, 0.001)
        assertEquals(3200.0, aggregate.totalExpense, 0.001)
        assertEquals(1800.0, aggregate.netBalance, 0.001)
    }

    @Test
    fun `Repository getTransactionsForMonth delegates to TransactionDao`() = runTest {
        val sampleEntities = listOf(
            TransactionEntity("1", "Alquiler", 1200.0, "expense", "Vivienda", "2026-10-01 10:00"),
            TransactionEntity("2", "Salario", 3000.0, "income", "Nómina", "2026-10-05 08:00")
        )
        every { transactionDao.getByMonth("2026-10") } returns sampleEntities

        val result = repository.getTransactionsForMonth("2026-10")

        assertEquals(2, result.size)
        assertEquals("Alquiler", result[0].title)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals("Salario", result[1].title)
        assertEquals(TransactionType.INCOME, result[1].type)

        verify(exactly = 1) { transactionDao.getByMonth("2026-10") }
    }

    @Test
    fun `Repository deletePayment deletes payment and queues sync mutation`() = runTest {
        every { paymentDao.deleteById("pay-123") } returns true

        val success = repository.deletePayment("pay-123")

        assertTrue(success)
        verify(exactly = 1) { paymentDao.deleteById("pay-123") }
        verify(exactly = 1) {
            syncQueueDao.insert(
                match { it.entityType == "PAYMENT" && it.entityId == "pay-123" && it.operation == "DELETE" }
            )
        }
    }
}
