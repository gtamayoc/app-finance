package com.gtc.app_finance.domain.model

data class Payment(
    val id: String,
    val creditId: String,
    val amount: Double,
    val date: String
)
