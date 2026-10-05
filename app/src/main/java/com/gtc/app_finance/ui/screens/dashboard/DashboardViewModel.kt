package com.gtc.app_finance.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtc.app_finance.data.database.TursoConfig
import com.gtc.app_finance.data.repository.FinanceRepository
import com.gtc.app_finance.domain.model.Credit
import com.gtc.app_finance.domain.model.FinancialSummary
import com.gtc.app_finance.domain.model.FullDatabaseStatus
import com.gtc.app_finance.domain.model.Transaction
import com.gtc.app_finance.domain.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    val summary: StateFlow<FinancialSummary> = repository.summary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary())

    val recentTransactions: StateFlow<List<Transaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val credits: StateFlow<List<Credit>> = repository.credits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dbStatus: StateFlow<FullDatabaseStatus> = repository.dbStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FullDatabaseStatus())

    init {
        viewModelScope.launch {
            repository.refreshData()
        }
    }

    fun addQuickTransaction(title: String, amount: Double, type: TransactionType, category: String) {
        viewModelScope.launch {
            repository.addTransaction(title, amount, type, category)
        }
    }

    fun testDatabaseConnection() {
        viewModelScope.launch {
            repository.checkDatabaseHealth()
        }
    }

    fun syncRemoteSchema() {
        viewModelScope.launch {
            repository.syncRemoteSchema()
        }
    }

    fun switchToken() {
        TursoConfig.switchToken()
        testDatabaseConnection()
    }
}
