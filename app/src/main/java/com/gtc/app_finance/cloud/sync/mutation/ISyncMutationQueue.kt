package com.gtc.app_finance.cloud.sync.mutation

interface ISyncMutationQueue {
    suspend fun enqueue(mutation: CloudMutation)
    suspend fun getPending(): List<CloudMutation>
    suspend fun remove(mutationId: String)
    suspend fun removeByEntity(collection: String, entityId: String)
    suspend fun getCount(): Int
    suspend fun clear()
}
