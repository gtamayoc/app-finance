package com.gtc.app_finance.domain.model

data class FinancialSummary(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalDebt: Double = 0.0,
    val activeCreditsCount: Int = 0
)
