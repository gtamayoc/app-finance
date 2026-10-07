package com.gtc.app_finance.cloud.sync.mutation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocalSyncMutationQueue : ISyncMutationQueue {

    private val mutex = Mutex()
    private val queue = mutableListOf<CloudMutation>()

    override suspend fun enqueue(mutation: CloudMutation): Unit = mutex.withLock {
        if (mutation.type == MutationType.DELETE) {
            queue.removeAll { it.collection == mutation.collection && it.entityId == mutation.entityId }
        } else if (mutation.type == MutationType.UPSERT) {
            queue.removeAll { it.collection == mutation.collection && it.entityId == mutation.entityId && it.type == MutationType.UPSERT }
        }
        queue.add(mutation)
    }

    override suspend fun getPending(): List<CloudMutation> = mutex.withLock {
        queue.toList()
    }

    override suspend fun remove(mutationId: String): Unit = mutex.withLock {
        queue.removeAll { it.id == mutationId }
    }

    override suspend fun removeByEntity(collection: String, entityId: String): Unit = mutex.withLock {
        queue.removeAll { it.collection == collection && it.entityId == entityId }
    }

    override suspend fun getCount(): Int = mutex.withLock {
        queue.size
    }

    override suspend fun clear(): Unit = mutex.withLock {
        queue.clear()
    }
}
