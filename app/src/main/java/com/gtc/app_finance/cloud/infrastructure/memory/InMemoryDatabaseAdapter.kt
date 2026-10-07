package com.gtc.app_finance.cloud.infrastructure.memory

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.DatabaseDocument
import com.gtc.app_finance.cloud.domain.ports.IDatabaseService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

class InMemoryDatabaseAdapter : IDatabaseService {

    private val tables = ConcurrentHashMap<String, ConcurrentHashMap<String, DatabaseDocument>>()
    private val mutationNotifier = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    override suspend fun getDocument(collection: String, id: String): CloudResult<DatabaseDocument> {
        val col = tables[collection]
        val doc = col?.get(id)
        return if (doc != null) {
            CloudResult.Success(doc)
        } else {
            CloudResult.Failure(CloudError.DatabaseError.DocumentNotFound(collection, id))
        }
    }

    override suspend fun saveDocument(
        collection: String,
        id: String,
        data: Map<String, Any?>
    ): CloudResult<Unit> {
        if (collection.isBlank() || id.isBlank()) {
            return CloudResult.Failure(
                CloudError.DatabaseError.InvalidQuery("Collection and document ID cannot be blank")
            )
        }
        val col = tables.computeIfAbsent(collection) { ConcurrentHashMap() }
        val doc = DatabaseDocument(
            id = id,
            collection = collection,
            data = data,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        col[id] = doc
        mutationNotifier.tryEmit(Unit)
        return CloudResult.Success(Unit)
    }

    override suspend fun deleteDocument(collection: String, id: String): CloudResult<Unit> {
        val col = tables[collection]
        return if (col != null && col.remove(id) != null) {
            mutationNotifier.tryEmit(Unit)
            CloudResult.Success(Unit)
        } else {
            CloudResult.Failure(CloudError.DatabaseError.DocumentNotFound(collection, id))
        }
    }

    override suspend fun queryDocuments(
        collection: String,
        filters: Map<String, Any?>
    ): CloudResult<List<DatabaseDocument>> {
        val col = tables[collection] ?: return CloudResult.Success(emptyList())

        if (filters.isEmpty()) {
            return CloudResult.Success(col.values.toList())
        }

        val filtered = col.values.filter { doc ->
            filters.all { (key, expectedValue) ->
                doc.data[key] == expectedValue
            }
        }
        return CloudResult.Success(filtered)
    }

    override fun streamDocument(collection: String, id: String): Flow<CloudResult<DatabaseDocument>> = flow {
        emit(getDocument(collection, id))
        mutationNotifier.collect {
            emit(getDocument(collection, id))
        }
    }.distinctUntilChanged()

    override fun streamDocuments(
        collection: String,
        filters: Map<String, Any?>
    ): Flow<CloudResult<List<DatabaseDocument>>> = flow {
        emit(queryDocuments(collection, filters))
        mutationNotifier.collect {
            emit(queryDocuments(collection, filters))
        }
    }.distinctUntilChanged()

    fun clear() {
        tables.clear()
        mutationNotifier.tryEmit(Unit)
    }
}
