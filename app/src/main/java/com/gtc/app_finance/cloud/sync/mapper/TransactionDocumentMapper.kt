package com.gtc.app_finance.cloud.sync.mapper

import com.gtc.app_finance.cloud.domain.model.DatabaseDocument
import com.gtc.app_finance.data.entity.TransactionEntity

object TransactionDocumentMapper {

    const val COLLECTION_NAME = "transactions"

    fun toDocument(
        entity: TransactionEntity,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    ): DatabaseDocument {
        val data = mutableMapOf<String, Any?>(
            "title" to entity.title,
            "amount" to entity.amount,
            "type" to entity.type,
            "category" to entity.category,
            "date" to entity.date,
            "updatedAt" to updatedAtEpochMs
        )
        if (entity.paymentId != null) {
            data["paymentId"] = entity.paymentId
        }
        return DatabaseDocument(
            id = entity.id,
            collection = COLLECTION_NAME,
            data = data,
            updatedAtEpochMs = updatedAtEpochMs
        )
    }

    fun toEntity(document: DatabaseDocument): TransactionEntity? {
        val data = document.data
        val title = data["title"] as? String ?: return null
        val amount = (data["amount"] as? Number)?.toDouble() ?: return null
        val type = data["type"] as? String ?: "expense"
        val category = data["category"] as? String ?: "General"
        val date = data["date"] as? String ?: return null
        val paymentId = data["paymentId"] as? String

        return TransactionEntity(
            id = document.id,
            title = title,
            amount = amount,
            type = type,
            category = category,
            date = date,
            paymentId = paymentId
        )
    }
}
