package com.gtc.app_finance.cloud.domain.model

data class DatabaseDocument(
    val id: String,
    val collection: String,
    val data: Map<String, Any?>,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
