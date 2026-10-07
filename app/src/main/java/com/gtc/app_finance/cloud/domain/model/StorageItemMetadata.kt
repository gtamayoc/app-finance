package com.gtc.app_finance.cloud.domain.model

data class StorageItemMetadata(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val downloadUrl: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val customMetadata: Map<String, String> = emptyMap()
)
