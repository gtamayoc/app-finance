package com.gtc.app_finance

import app.cash.turbine.test
import com.gtc.app_finance.data.repository.FinanceRepository
import com.gtc.app_finance.domain.model.FinancialSummary
import com.gtc.app_finance.domain.model.Transaction
import com.gtc.app_finance.domain.model.TransactionType
import com.gtc.app_finance.ui.screens.analytics.AnalyticsViewModel
import com.gtc.app_finance.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AnalyticsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: FinanceRepository = mockk(relaxed = true)
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    private val summaryFlow = MutableStateFlow(FinancialSummary(totalBalance = 1000.0, totalIncome = 1500.0, totalExpense = 500.0))

    private lateinit var viewModel: AnalyticsViewModel

    @Before
    fun setUp() {
        every { repository.transactions } returns transactionsFlow
        every { repository.summary } returns summaryFlow
        viewModel = AnalyticsViewModel(repository)
    }

    @Test
    fun `given empty transactions when categoryExpenses observed should emit empty list`() = runTest {
        viewModel.categoryExpenses.test {
            val items = awaitItem()
            assertTrue(items.isEmpty())
        }
    }

    @Test
    fun `given expense transactions when observed should aggregate categories and percentages correctly`() = runTest {
        val testTransactions = listOf(
            Transaction(id = "1", title = "Cena", amount = 100.0, type = TransactionType.EXPENSE, category = "Comida", date = "2026-10-01"),
            Transaction(id = "2", title = "Almuerzo", amount = 100.0, type = TransactionType.EXPENSE, category = "Comida", date = "2026-10-02"),
            Transaction(id = "3", title = "Uber", amount = 50.0, type = TransactionType.EXPENSE, category = "Transporte", date = "2026-10-03"),
            Transaction(id = "4", title = "Salario", amount = 2000.0, type = TransactionType.INCOME, category = "Ingreso", date = "2026-10-04")
        )

        viewModel.categoryExpenses.test {
            // initial empty emission
            assertEquals(0, awaitItem().size)

            transactionsFlow.value = testTransactions
            val expenses = awaitItem()

            // Total expenses = 250.0 (Comida = 200.0, Transporte = 50.0). Income is excluded.
            assertEquals(2, expenses.size)
            assertEquals("Comida", expenses[0].category)
            assertEquals(200.0, expenses[0].totalAmount, 0.001)
            assertEquals(0.8f, expenses[0].percentage, 0.01f) // 200 / 250 = 80%

            assertEquals("Transporte", expenses[1].category)
            assertEquals(50.0, expenses[1].totalAmount, 0.001)
            assertEquals(0.2f, expenses[1].percentage, 0.01f) // 50 / 250 = 20%
        }
    }
}
