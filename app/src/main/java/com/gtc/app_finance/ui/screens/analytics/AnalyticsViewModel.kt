package com.gtc.app_finance.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtc.app_finance.data.repository.FinanceRepository
import com.gtc.app_finance.domain.model.FinancialSummary
import com.gtc.app_finance.domain.model.Transaction
import com.gtc.app_finance.domain.model.TransactionType
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Immutable
data class CategoryExpense(
    val category: String,
    val totalAmount: Double,
    val percentage: Float,
    val color: Color
)

class AnalyticsViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    val summary: StateFlow<FinancialSummary> = repository.summary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.summary.value)

    private val chartColors = listOf(
        Color(0xFFFF453A),
        Color(0xFF0A84FF),
        Color(0xFFFF9F0A),
        Color(0xFFBF5AF2),
        Color(0xFF64D2FF),
        Color(0xFFFFD60A)
    )

    private fun calculateCategoryExpenses(list: List<Transaction>): List<CategoryExpense> {
        val expenses = list.filter { it.type == TransactionType.EXPENSE }
        val totalExpenseAmount = expenses.sumOf { it.amount }

        return if (totalExpenseAmount == 0.0) {
            emptyList()
        } else {
            val grouped = expenses.groupBy { it.category }
            grouped.entries.mapIndexed { index, entry ->
                val categorySum = entry.value.sumOf { it.amount }
                val percentage = (categorySum / totalExpenseAmount).toFloat()
                CategoryExpense(
                    category = entry.key,
                    totalAmount = categorySum,
                    percentage = percentage,
                    color = chartColors[index % chartColors.size]
                )
            }.sortedByDescending { it.totalAmount }
        }
    }

    val categoryExpenses: StateFlow<List<CategoryExpense>> = repository.transactions
        .map { calculateCategoryExpenses(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = calculateCategoryExpenses(repository.transactions.value)
        )
}
