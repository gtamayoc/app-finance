package com.gtc.app_finance.domain.model

data class MonthlyAggregate(
    val yearMonth: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double
)
