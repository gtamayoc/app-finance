package com.gtc.app_finance.cloud.domain.ports

import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.DatabaseDocument

interface IDatabaseService {
    suspend fun getDocument(collection: String, id: String): CloudResult<DatabaseDocument>
    suspend fun saveDocument(collection: String, id: String, data: Map<String, Any?>): CloudResult<Unit>
    suspend fun deleteDocument(collection: String, id: String): CloudResult<Unit>
    suspend fun queryDocuments(collection: String, filters: Map<String, Any?> = emptyMap()): CloudResult<List<DatabaseDocument>>
}
