package com.gtc.app_finance.domain.model

import androidx.compose.runtime.Immutable

enum class TransactionType {
    INCOME, EXPENSE
}

@Immutable
data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val date: String
)

