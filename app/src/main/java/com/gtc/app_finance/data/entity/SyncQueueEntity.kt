package com.gtc.app_finance.data.entity

data class SyncQueueEntity(
    val id: Long = 0,
    val entityType: String, // "TRANSACTION", "CREDIT", "PAYMENT"
    val entityId: String,
    val operation: String,  // "INSERT", "UPDATE", "DELETE"
    val sqlCommand: String,
    val createdAt: Long = System.currentTimeMillis()
)
