package com.gtc.app_finance.cloud.sync.mutation

import java.util.UUID

data class CloudMutation(
    val id: String = UUID.randomUUID().toString(),
    val collection: String,
    val entityId: String,
    val type: MutationType,
    val payload: Map<String, Any?>? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
