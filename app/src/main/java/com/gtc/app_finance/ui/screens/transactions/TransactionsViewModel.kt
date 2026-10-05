package com.gtc.app_finance.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtc.app_finance.data.repository.FinanceRepository
import com.gtc.app_finance.domain.model.Transaction
import com.gtc.app_finance.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionsViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val filterTypeIndex = MutableStateFlow(0) // 0 = Todos, 1 = Ingresos, 2 = Gastos

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        repository.transactions,
        searchQuery,
        filterTypeIndex
    ) { transactions, query, filter ->
        transactions.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.title.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true)

            val matchesType = when (filter) {
                1 -> tx.type == TransactionType.INCOME
                2 -> tx.type == TransactionType.EXPENSE
                else -> true
            }

            matchesQuery && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onFilterSelected(index: Int) {
        filterTypeIndex.value = index
    }

    fun addTransaction(title: String, amount: Double, type: TransactionType, category: String) {
        viewModelScope.launch {
            repository.addTransaction(title, amount, type, category)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }
}
