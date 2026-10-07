package com.gtc.app_finance.cloud.sync.mapper

import com.gtc.app_finance.cloud.domain.model.DatabaseDocument
import com.gtc.app_finance.data.entity.PaymentEntity

object PaymentDocumentMapper {

    const val COLLECTION_NAME = "payments"

    fun toDocument(
        entity: PaymentEntity,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    ): DatabaseDocument {
        val data = mapOf(
            "creditId" to entity.creditId,
            "amount" to entity.amount,
            "date" to entity.date,
            "updatedAt" to updatedAtEpochMs
        )
        return DatabaseDocument(
            id = entity.id,
            collection = COLLECTION_NAME,
            data = data,
            updatedAtEpochMs = updatedAtEpochMs
        )
    }

    fun toEntity(document: DatabaseDocument): PaymentEntity? {
        val data = document.data
        val creditId = data["creditId"] as? String ?: return null
        val amount = (data["amount"] as? Number)?.toDouble() ?: return null
        val date = data["date"] as? String ?: return null

        return PaymentEntity(
            id = document.id,
            creditId = creditId,
            amount = amount,
            date = date
        )
    }
}
