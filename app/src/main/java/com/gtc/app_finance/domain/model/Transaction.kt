package com.gtc.app_finance.domain.model

enum class TransactionType {
    INCOME, EXPENSE
}

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val date: String
)
