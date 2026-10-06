package com.gtc.app_finance.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtc.app_finance.data.repository.FinanceRepository
import com.gtc.app_finance.domain.model.Credit
import com.gtc.app_finance.domain.model.FinancialSummary
import com.gtc.app_finance.domain.model.FullDatabaseStatus
import com.gtc.app_finance.domain.model.Transaction
import com.gtc.app_finance.domain.model.TransactionType
import com.gtc.app_finance.ui.components.DiagnosticUiMapper
import com.gtc.app_finance.ui.components.DiagnosticUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: FinanceRepository,
    private val enableAutoSync: Boolean = true
) : ViewModel() {

    val isInitialLoading: StateFlow<Boolean> = repository.isInitialLoading
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.isInitialLoading.value)

    val summary: StateFlow<FinancialSummary> = repository.summary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.summary.value)

    val recentTransactions: StateFlow<List<Transaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.transactions.value)

    val credits: StateFlow<List<Credit>> = repository.credits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.credits.value)

    val dbStatus: StateFlow<FullDatabaseStatus> = repository.dbStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.dbStatus.value)

    val diagnosticUiState: StateFlow<DiagnosticUiState> = repository.dbStatus
        .map { status ->
            if (DiagnosticUiMapper.isDataValid(status)) {
                DiagnosticUiState.Success(status)
            } else {
                DiagnosticUiState.Error()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = if (DiagnosticUiMapper.isDataValid(repository.dbStatus.value)) {
                DiagnosticUiState.Success(repository.dbStatus.value)
            } else {
                DiagnosticUiState.Loading
            }
        )

    init {
        viewModelScope.launch {
            repository.refreshData()
            if (enableAutoSync) {
                while (isActive) {
                    kotlinx.coroutines.delay(30_000)
                    repository.syncData()
                }
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            repository.triggerShimmerReload()
            repository.syncData()
        }
    }

    fun clearAllData(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = repository.clearAllData()
            onComplete(ok)
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
        viewModelScope.launch {
            repository.switchRemoteToken()
        }
    }
}
