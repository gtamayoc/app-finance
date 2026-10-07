package com.gtc.app_finance.cloud.domain.model

data class CloudUser(
    val id: String,
    val email: String?,
    val displayName: String? = null,
    val isAnonymous: Boolean = false,
    val customClaims: Map<String, Any?> = emptyMap()
)
