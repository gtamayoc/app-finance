package com.gtc.app_finance.cloud.sync

import com.gtc.app_finance.cloud.domain.model.DatabaseDocument
import com.gtc.app_finance.cloud.sync.mapper.CreditDocumentMapper
import com.gtc.app_finance.cloud.sync.mapper.PaymentDocumentMapper
import com.gtc.app_finance.cloud.sync.mapper.TransactionDocumentMapper
import com.gtc.app_finance.data.entity.CreditEntity
import com.gtc.app_finance.data.entity.PaymentEntity
import com.gtc.app_finance.data.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DocumentMappersTest {

    // --- TransactionDocumentMapper Tests ---

    @Test
    fun `TransactionDocumentMapper toDocument correctly serializes entity`() {
        val entity = TransactionEntity(
            id = "tx-123",
            title = "Compra Supermercado",
            amount = 145.50,
            type = "expense",
            category = "Alimentación",
            date = "2026-10-07 14:00"
        )
        val fixedTime = 1760000000000L

        val doc = TransactionDocumentMapper.toDocument(entity, updatedAtEpochMs = fixedTime)

        assertEquals("tx-123", doc.id)
        assertEquals(TransactionDocumentMapper.COLLECTION_NAME, doc.collection)
        assertEquals(fixedTime, doc.updatedAtEpochMs)
        assertEquals("Compra Supermercado", doc.data["title"])
        assertEquals(145.50, doc.data["amount"])
        assertEquals("expense", doc.data["type"])
        assertEquals("Alimentación", doc.data["category"])
        assertEquals("2026-10-07 14:00", doc.data["date"])
        assertEquals(fixedTime, doc.data["updatedAt"])
    }

    @Test
    fun `TransactionDocumentMapper toEntity deserializes document and handles numeric conversions`() {
        val doc = DatabaseDocument(
            id = "tx-456",
            collection = "transactions",
            data = mapOf(
                "title" to "Salario",
                "amount" to 2500, // Int from json/firestore
                "type" to "income",
                "category" to "Nómina",
                "date" to "2026-10-01 08:00"
            ),
            updatedAtEpochMs = 1760000000000L
        )

        val entity = TransactionDocumentMapper.toEntity(doc)

        assertNotNull(entity)
        assertEquals("tx-456", entity?.id)
        assertEquals("Salario", entity?.title)
        assertEquals(2500.0, entity?.amount)
        assertEquals("income", entity?.type)
        assertEquals("Nómina", entity?.category)
        assertEquals("2026-10-01 08:00", entity?.date)
    }

    @Test
    fun `TransactionDocumentMapper correctly preserves paymentId in round trip`() {
        val entity = TransactionEntity(
            id = "tx-abono-1",
            title = "Abono Préstamo",
            amount = 300.0,
            type = "expense",
            category = "Pago Crédito",
            date = "2026-10-07 10:00",
            paymentId = "pay-linked-1"
        )
        val doc = TransactionDocumentMapper.toDocument(entity)
        assertEquals("pay-linked-1", doc.data["paymentId"])

        val backEntity = TransactionDocumentMapper.toEntity(doc)
        assertNotNull(backEntity)
        assertEquals("pay-linked-1", backEntity?.paymentId)
    }

    @Test
    fun `TransactionDocumentMapper toEntity returns null when mandatory fields are missing`() {
        val missingTitleDoc = DatabaseDocument(
            id = "tx-bad-1",
            collection = "transactions",
            data = mapOf("amount" to 50.0, "date" to "2026-10-07")
        )
        assertNull(TransactionDocumentMapper.toEntity(missingTitleDoc))

        val missingAmountDoc = DatabaseDocument(
            id = "tx-bad-2",
            collection = "transactions",
            data = mapOf("title" to "Test", "date" to "2026-10-07")
        )
        assertNull(TransactionDocumentMapper.toEntity(missingAmountDoc))

        val missingDateDoc = DatabaseDocument(
            id = "tx-bad-3",
            collection = "transactions",
            data = mapOf("title" to "Test", "amount" to 50.0)
        )
        assertNull(TransactionDocumentMapper.toEntity(missingDateDoc))
    }

    // --- CreditDocumentMapper Tests ---

    @Test
    fun `CreditDocumentMapper toDocument correctly serializes entity`() {
        val entity = CreditEntity(
            id = "cred-1",
            title = "Crédito Vehículo",
            totalAmount = 15000.0,
            remainingAmount = 8500.0,
            dueDate = "2027-05-15"
        )
        val fixedTime = 1760000000000L

        val doc = CreditDocumentMapper.toDocument(entity, updatedAtEpochMs = fixedTime)

        assertEquals("cred-1", doc.id)
        assertEquals(CreditDocumentMapper.COLLECTION_NAME, doc.collection)
        assertEquals("Crédito Vehículo", doc.data["title"])
        assertEquals(15000.0, doc.data["totalAmount"])
        assertEquals(8500.0, doc.data["remainingAmount"])
        assertEquals("2027-05-15", doc.data["dueDate"])
        assertEquals(fixedTime, doc.data["updatedAt"])
    }

    @Test
    fun `CreditDocumentMapper toEntity deserializes document and defaults remaining amount`() {
        val docWithoutRemaining = DatabaseDocument(
            id = "cred-2",
            collection = "credits",
            data = mapOf(
                "title" to "Préstamo Personal",
                "totalAmount" to 5000,
                "dueDate" to "2026-12-31"
            )
        )

        val entity = CreditDocumentMapper.toEntity(docWithoutRemaining)

        assertNotNull(entity)
        assertEquals("cred-2", entity?.id)
        assertEquals("Préstamo Personal", entity?.title)
        assertEquals(5000.0, entity?.totalAmount)
        assertEquals(5000.0, entity?.remainingAmount) // Defaults to totalAmount
        assertEquals("2026-12-31", entity?.dueDate)
    }

    @Test
    fun `CreditDocumentMapper toEntity returns null on missing mandatory attributes`() {
        val invalidDoc = DatabaseDocument(
            id = "cred-invalid",
            collection = "credits",
            data = mapOf("title" to "Solo título")
        )
        assertNull(CreditDocumentMapper.toEntity(invalidDoc))
    }

    // --- PaymentDocumentMapper Tests ---

    @Test
    fun `PaymentDocumentMapper toDocument correctly serializes entity`() {
        val entity = PaymentEntity(
            id = "pay-1",
            creditId = "cred-1",
            amount = 500.0,
            date = "2026-10-05 10:30"
        )
        val fixedTime = 1760000000000L

        val doc = PaymentDocumentMapper.toDocument(entity, updatedAtEpochMs = fixedTime)

        assertEquals("pay-1", doc.id)
        assertEquals(PaymentDocumentMapper.COLLECTION_NAME, doc.collection)
        assertEquals("cred-1", doc.data["creditId"])
        assertEquals(500.0, doc.data["amount"])
        assertEquals("2026-10-05 10:30", doc.data["date"])
        assertEquals(fixedTime, doc.data["updatedAt"])
    }

    @Test
    fun `PaymentDocumentMapper toEntity deserializes document successfully`() {
        val doc = DatabaseDocument(
            id = "pay-2",
            collection = "payments",
            data = mapOf(
                "creditId" to "cred-1",
                "amount" to 250,
                "date" to "2026-10-06 11:00"
            )
        )

        val entity = PaymentDocumentMapper.toEntity(doc)

        assertNotNull(entity)
        assertEquals("pay-2", entity?.id)
        assertEquals("cred-1", entity?.creditId)
        assertEquals(250.0, entity?.amount)
        assertEquals("2026-10-06 11:00", entity?.date)
    }

    @Test
    fun `PaymentDocumentMapper toEntity returns null when creditId or amount is missing`() {
        val missingCreditId = DatabaseDocument(
            id = "pay-bad-1",
            collection = "payments",
            data = mapOf("amount" to 100.0, "date" to "2026-10-07")
        )
        assertNull(PaymentDocumentMapper.toEntity(missingCreditId))

        val missingAmount = DatabaseDocument(
            id = "pay-bad-2",
            collection = "payments",
            data = mapOf("creditId" to "cred-1", "date" to "2026-10-07")
        )
        assertNull(PaymentDocumentMapper.toEntity(missingAmount))
    }
}
