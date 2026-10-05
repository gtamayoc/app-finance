package com.gtc.app_finance.ui.components

import com.gtc.app_finance.domain.model.FullDatabaseStatus

/**
 * Sealed UI State representing the database diagnostic screen state.
 * Processed and validated at the ViewModel layer to avoid business validation inside Composables.
 */
sealed interface DiagnosticUiState {
    data object Loading : DiagnosticUiState
    data class Success(val status: FullDatabaseStatus) : DiagnosticUiState
    data class Error(val message: String? = null) : DiagnosticUiState
}
