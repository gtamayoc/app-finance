package com.gtc.app_finance.cloud.infrastructure.firebase

import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.DatabaseDocument
import com.gtc.app_finance.cloud.domain.ports.IDatabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class FirebaseDatabaseAdapter(
    private val errorMapper: FirebaseErrorMapper = FirebaseErrorMapper()
) : IDatabaseService {

    private val firestoreData = ConcurrentHashMap<String, ConcurrentHashMap<String, DatabaseDocument>>()
    private val realtimeNotifier = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    override suspend fun getDocument(collection: String, id: String): CloudResult<DatabaseDocument> = withContext(Dispatchers.IO) {
        val doc = firestoreData[collection]?.get(id)
        if (doc != null) {
            CloudResult.Success(doc)
        } else {
            CloudResult.Failure(
                errorMapper.map(Exception("not-found: Document $id in Firestore collection $collection"))
            )
        }
    }

    override suspend fun saveDocument(
        collection: String,
        id: String,
        data: Map<String, Any?>
    ): CloudResult<Unit> = withContext(Dispatchers.IO) {
        if (collection.isBlank() || id.isBlank()) {
            return@withContext CloudResult.Failure(
                errorMapper.map(Exception("invalid-argument: Collection and ID must be non-empty"))
            )
        }
        val col = firestoreData.computeIfAbsent(collection) { ConcurrentHashMap() }
        val doc = DatabaseDocument(
            id = id,
            collection = collection,
            data = data,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        col[id] = doc
        realtimeNotifier.tryEmit(Unit)
        CloudResult.Success(Unit)
    }

    override suspend fun deleteDocument(collection: String, id: String): CloudResult<Unit> = withContext(Dispatchers.IO) {
        val col = firestoreData[collection]
        if (col != null && col.remove(id) != null) {
            realtimeNotifier.tryEmit(Unit)
            CloudResult.Success(Unit)
        } else {
            CloudResult.Failure(
                errorMapper.map(Exception("not-found: Document $id not found"))
            )
        }
    }

    override suspend fun queryDocuments(
        collection: String,
        filters: Map<String, Any?>
    ): CloudResult<List<DatabaseDocument>> = withContext(Dispatchers.IO) {
        val col = firestoreData[collection] ?: return@withContext CloudResult.Success(emptyList())
        if (filters.isEmpty()) {
            return@withContext CloudResult.Success(col.values.toList())
        }
        val filtered = col.values.filter { doc ->
            filters.all { (key, expectedValue) -> doc.data[key] == expectedValue }
        }
        CloudResult.Success(filtered)
    }

    override fun streamDocument(collection: String, id: String): Flow<CloudResult<DatabaseDocument>> = flow {
        emit(getDocument(collection, id))
        realtimeNotifier.collect {
            emit(getDocument(collection, id))
        }
    }.distinctUntilChanged()

    override fun streamDocuments(
        collection: String,
        filters: Map<String, Any?>
    ): Flow<CloudResult<List<DatabaseDocument>>> = flow {
        emit(queryDocuments(collection, filters))
        realtimeNotifier.collect {
            emit(queryDocuments(collection, filters))
        }
    }.distinctUntilChanged()
}
