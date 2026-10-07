package com.gtc.app_finance.cloud.sync.mapper

import com.gtc.app_finance.cloud.domain.model.DatabaseDocument
import com.gtc.app_finance.data.entity.CreditEntity

object CreditDocumentMapper {

    const val COLLECTION_NAME = "credits"

    fun toDocument(
        entity: CreditEntity,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    ): DatabaseDocument {
        val data = mapOf(
            "title" to entity.title,
            "totalAmount" to entity.totalAmount,
            "remainingAmount" to entity.remainingAmount,
            "dueDate" to entity.dueDate,
            "updatedAt" to updatedAtEpochMs
        )
        return DatabaseDocument(
            id = entity.id,
            collection = COLLECTION_NAME,
            data = data,
            updatedAtEpochMs = updatedAtEpochMs
        )
    }

    fun toEntity(document: DatabaseDocument): CreditEntity? {
        val data = document.data
        val title = data["title"] as? String ?: return null
        val totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: return null
        val remainingAmount = (data["remainingAmount"] as? Number)?.toDouble() ?: totalAmount
        val dueDate = data["dueDate"] as? String ?: return null

        return CreditEntity(
            id = document.id,
            title = title,
            totalAmount = totalAmount,
            remainingAmount = remainingAmount,
            dueDate = dueDate
        )
    }
}
