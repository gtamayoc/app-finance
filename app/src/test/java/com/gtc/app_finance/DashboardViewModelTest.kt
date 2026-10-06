package com.gtc.app_finance

import app.cash.turbine.test
import com.gtc.app_finance.data.repository.FinanceRepository
import com.gtc.app_finance.domain.model.ConnectionStatus
import com.gtc.app_finance.domain.model.FinancialSummary
import com.gtc.app_finance.domain.model.FullDatabaseStatus
import com.gtc.app_finance.domain.model.LocalDbDiagnostic
import com.gtc.app_finance.domain.model.RemoteDbDiagnostic
import com.gtc.app_finance.domain.model.Transaction
import com.gtc.app_finance.domain.model.TransactionType
import com.gtc.app_finance.ui.components.DiagnosticUiState
import com.gtc.app_finance.ui.screens.dashboard.DashboardViewModel
import com.gtc.app_finance.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: FinanceRepository = mockk(relaxed = true)

    private val summaryFlow = MutableStateFlow(FinancialSummary(totalBalance = 5000.0, totalIncome = 7000.0, totalExpense = 2000.0))
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    private val dbStatusFlow = MutableStateFlow(
        FullDatabaseStatus(
            local = LocalDbDiagnostic(),
            remote = RemoteDbDiagnostic(status = ConnectionStatus.CONNECTED, latencyMs = 45)
        )
    )

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        every { repository.summary } returns summaryFlow
        every { repository.transactions } returns transactionsFlow
        every { repository.dbStatus } returns dbStatusFlow

        viewModel = DashboardViewModel(repository, enableAutoSync = false)
    }

    @Test
    fun `given initialization when created should call refreshData`() = runTest {
        coVerify(atLeast = 1) { repository.refreshData() }
    }

    @Test
    fun `given summary from repository when observed should emit valid data`() = runTest {
        viewModel.summary.test {
            val item = awaitItem()
            assertEquals(5000.0, item.totalBalance, 0.001)
            assertEquals(7000.0, item.totalIncome, 0.001)
            assertEquals(2000.0, item.totalExpense, 0.001)
        }
    }

    @Test
    fun `given valid dbStatus when diagnosticUiState observed should emit Success`() = runTest {
        viewModel.diagnosticUiState.test {
            val state = awaitItem()
            assertTrue(state is DiagnosticUiState.Success)
            assertEquals(ConnectionStatus.CONNECTED, (state as DiagnosticUiState.Success).status.remote.status)
        }
    }

    @Test
    fun `given addQuickTransaction when invoked should delegate to repository`() = runTest {
        viewModel.addQuickTransaction("Almuerzo", 25.0, TransactionType.EXPENSE, "Comida")

        coVerify(exactly = 1) {
            repository.addTransaction("Almuerzo", 25.0, TransactionType.EXPENSE, "Comida")
        }
    }

    @Test
    fun `given syncData when invoked should delegate to repository`() = runTest {
        viewModel.syncData()

        coVerify(atLeast = 1) {
            repository.syncData()
        }
    }

    @Test
    fun `given clearAllData when executed should trigger repository and return success in callback`() = runTest {
        coEvery { repository.clearAllData() } returns true

        var callbackResult: Boolean? = null
        viewModel.clearAllData { success ->
            callbackResult = success
        }

        coVerify(exactly = 1) { repository.clearAllData() }
        assertEquals(true, callbackResult)
    }
}
