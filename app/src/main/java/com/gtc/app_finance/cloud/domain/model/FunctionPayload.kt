package com.gtc.app_finance.cloud.domain.model

data class FunctionPayload(
    val rawData: String,
    val headers: Map<String, String> = emptyMap()
)
