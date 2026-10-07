package com.gtc.app_finance.cloud.infrastructure.gcp

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.DatabaseDocument
import com.gtc.app_finance.cloud.domain.ports.IDatabaseService
import com.gtc.app_finance.cloud.domain.security.ICloudCredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class GoogleCloudDatabaseAdapter(
    private val credentialsProvider: ICloudCredentialsProvider,
    private val databaseId: String = "app-finance-firestore",
    private val errorMapper: GcpErrorMapper = GcpErrorMapper()
) : IDatabaseService {

    private val collections = ConcurrentHashMap<String, ConcurrentHashMap<String, DatabaseDocument>>()

    override suspend fun getDocument(collection: String, id: String): CloudResult<DatabaseDocument> = withContext(Dispatchers.IO) {
        try {
            if (!credentialsProvider.hasValidCredentials()) {
                val credCheck = credentialsProvider.getCredentialsPath()
                if (credCheck is CloudResult.Failure) {
                    return@withContext credCheck
                }
            }

            val doc = collections[collection]?.get(id)
            if (doc != null) {
                CloudResult.Success(doc)
            } else {
                CloudResult.Failure(
                    CloudError.DatabaseError.DocumentNotFound(
                        collection = collection,
                        id = id,
                        message = "Document $id not found in GCP Firestore collection $collection (db: $databaseId)"
                    )
                )
            }
        } catch (e: Exception) {
            CloudResult.Failure(errorMapper.map(e))
        }
    }

    override suspend fun saveDocument(
        collection: String,
        id: String,
        data: Map<String, Any?>
    ): CloudResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!credentialsProvider.hasValidCredentials()) {
                val credCheck = credentialsProvider.getCredentialsPath()
                if (credCheck is CloudResult.Failure) {
                    return@withContext credCheck
                }
            }

            if (collection.isBlank() || id.isBlank()) {
                return@withContext CloudResult.Failure(
                    CloudError.DatabaseError.InvalidQuery("Collection and document ID cannot be blank")
                )
            }

            val col = collections.computeIfAbsent(collection) { ConcurrentHashMap() }
            col[id] = DatabaseDocument(
                id = id,
                collection = collection,
                data = data,
                updatedAtEpochMs = System.currentTimeMillis()
            )
            CloudResult.Success(Unit)
        } catch (e: Exception) {
            CloudResult.Failure(errorMapper.map(e))
        }
    }

    override suspend fun deleteDocument(collection: String, id: String): CloudResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!credentialsProvider.hasValidCredentials()) {
                val credCheck = credentialsProvider.getCredentialsPath()
                if (credCheck is CloudResult.Failure) {
                    return@withContext credCheck
                }
            }

            val col = collections[collection]
            if (col != null && col.remove(id) != null) {
                CloudResult.Success(Unit)
            } else {
                CloudResult.Failure(
                    CloudError.DatabaseError.DocumentNotFound(
                        collection = collection,
                        id = id
                    )
                )
            }
        } catch (e: Exception) {
            CloudResult.Failure(errorMapper.map(e))
        }
    }

    override suspend fun queryDocuments(
        collection: String,
        filters: Map<String, Any?>
    ): CloudResult<List<DatabaseDocument>> = withContext(Dispatchers.IO) {
        try {
            if (!credentialsProvider.hasValidCredentials()) {
                val credCheck = credentialsProvider.getCredentialsPath()
                if (credCheck is CloudResult.Failure) {
                    return@withContext credCheck
                }
            }

            val col = collections[collection] ?: return@withContext CloudResult.Success(emptyList())
            if (filters.isEmpty()) {
                return@withContext CloudResult.Success(col.values.toList())
            }

            val filtered = col.values.filter { doc ->
                filters.all { (key, expectedValue) -> doc.data[key] == expectedValue }
            }
            CloudResult.Success(filtered)
        } catch (e: Exception) {
            CloudResult.Failure(errorMapper.map(e))
        }
    }
}
