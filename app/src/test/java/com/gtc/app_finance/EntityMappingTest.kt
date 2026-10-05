package com.gtc.app_finance

import com.gtc.app_finance.data.entity.CreditEntity
import com.gtc.app_finance.data.entity.PaymentEntity
import com.gtc.app_finance.data.entity.TransactionEntity
import com.gtc.app_finance.domain.model.Credit
import com.gtc.app_finance.domain.model.Payment
import com.gtc.app_finance.domain.model.Transaction
import com.gtc.app_finance.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityMappingTest {

    @Test
    fun `TransactionEntity toDomain maps income type correctly`() {
        val entity = TransactionEntity(
            id = "tx-1",
            title = "Nómina",
            amount = 2500000.0,
            type = "income",
            category = "Salario",
            date = "2026-10-01 10:00"
        )
        val domain = entity.toDomain()

        assertEquals("tx-1", domain.id)
        assertEquals("Nómina", domain.title)
        assertEquals(2500000.0, domain.amount, 0.001)
        assertEquals(TransactionType.INCOME, domain.type)
        assertEquals("Salario", domain.category)
        assertEquals("2026-10-01 10:00", domain.date)
    }

    @Test
    fun `TransactionEntity toDomain maps expense type case-insensitively`() {
        val entity = TransactionEntity(
            id = "tx-2",
            title = "Restaurante",
            amount = 65000.0,
            type = "EXPENSE",
            category = "Comida",
            date = "2026-10-02 12:00"
        )
        val domain = entity.toDomain()
        assertEquals(TransactionType.EXPENSE, domain.type)
    }

    @Test
    fun `TransactionEntity fromDomain maps domain to entity`() {
        val domain = Transaction(
            id = "tx-3",
            title = "Gasolina",
            amount = 80000.0,
            type = TransactionType.EXPENSE,
            category = "Transporte",
            date = "2026-10-03 15:00"
        )
        val entity = TransactionEntity.fromDomain(domain)

        assertEquals("tx-3", entity.id)
        assertEquals("expense", entity.type)
        assertEquals(80000.0, entity.amount, 0.001)
    }

    @Test
    fun `CreditEntity toDomain and calculated properties work as expected`() {
        val activeEntity = CreditEntity(
            id = "cr-1",
            title = "Tarjeta Visa",
            totalAmount = 2000000.0,
            remainingAmount = 1000000.0,
            dueDate = "2026-12-01"
        )
        val activeDomain = activeEntity.toDomain()

        assertFalse(activeDomain.isPaid)
        assertEquals(0.5f, activeDomain.progress, 0.001f)

        val paidEntity = CreditEntity(
            id = "cr-2",
            title = "Préstamo Banco",
            totalAmount = 1000000.0,
            remainingAmount = 0.0,
            dueDate = "2026-10-01"
        )
        val paidDomain = paidEntity.toDomain()

        assertTrue(paidDomain.isPaid)
        assertEquals(1.0f, paidDomain.progress, 0.001f)
    }

    @Test
    fun `PaymentEntity toDomain and fromDomain map properly`() {
        val payment = Payment(
            id = "pay-1",
            creditId = "cr-1",
            amount = 500000.0,
            date = "2026-10-05 14:00"
        )
        val entity = PaymentEntity.fromDomain(payment)
        assertEquals("pay-1", entity.id)
        assertEquals("cr-1", entity.creditId)
        assertEquals(500000.0, entity.amount, 0.001)

        val restored = entity.toDomain()
        assertEquals(payment, restored)
    }
}
