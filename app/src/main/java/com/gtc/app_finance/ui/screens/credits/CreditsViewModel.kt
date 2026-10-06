package com.gtc.app_finance.ui.screens.credits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtc.app_finance.data.repository.FinanceRepository
import com.gtc.app_finance.domain.model.Credit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CreditsViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    val credits: StateFlow<List<Credit>> = repository.credits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.credits.value)

    fun addCredit(title: String, totalAmount: Double, dueDate: String) {
        viewModelScope.launch {
            repository.addCredit(title, totalAmount, dueDate)
        }
    }

    fun recordPayment(creditId: String, amount: Double) {
        viewModelScope.launch {
            repository.recordPayment(creditId, amount)
        }
    }

    fun deleteCredit(id: String) {
        viewModelScope.launch {
            repository.deleteCredit(id)
        }
    }
}
