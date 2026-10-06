package com.gtc.app_finance

import app.cash.turbine.test
import com.gtc.app_finance.data.repository.FinanceRepository
import com.gtc.app_finance.domain.model.Transaction
import com.gtc.app_finance.domain.model.TransactionType
import com.gtc.app_finance.ui.screens.transactions.TransactionsViewModel
import com.gtc.app_finance.util.MainDispatcherRule
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

class TransactionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: FinanceRepository = mockk(relaxed = true)
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())

    private lateinit var viewModel: TransactionsViewModel

    private val sampleTransactions = listOf(
        Transaction(id = "tx-1", title = "Salario Empresa", amount = 3500.0, type = TransactionType.INCOME, category = "Nómina", date = "2026-10-01"),
        Transaction(id = "tx-2", title = "Supermercado Éxito", amount = 150.0, type = TransactionType.EXPENSE, category = "Alimentación", date = "2026-10-02"),
        Transaction(id = "tx-3", title = "Freelance UX", amount = 600.0, type = TransactionType.INCOME, category = "Servicios", date = "2026-10-03"),
        Transaction(id = "tx-4", title = "Restaurante Italiano", amount = 85.0, type = TransactionType.EXPENSE, category = "Restaurante", date = "2026-10-04")
    )

    @Before
    fun setUp() {
        every { repository.transactions } returns transactionsFlow
        transactionsFlow.value = sampleTransactions
        viewModel = TransactionsViewModel(repository)
    }

    @Test
    fun `given initial state when initialized should emit all transactions`() = runTest {
        viewModel.filteredTransactions.test {
            val items = awaitItem()
            assertEquals(4, items.size)
            assertEquals("Salario Empresa", items[0].title)
        }
    }

    @Test
    fun `given transactions when search query applied should filter matching title or category`() = runTest {
        viewModel.filteredTransactions.test {
            assertEquals(4, awaitItem().size)

            viewModel.onSearchQueryChanged("éxito")
            val filtered = awaitItem()
            assertEquals(1, filtered.size)
            assertEquals("Supermercado Éxito", filtered[0].title)

            viewModel.onSearchQueryChanged("Freelance")
            val byCategory = awaitItem()
            assertEquals(1, byCategory.size)
            assertEquals("tx-3", byCategory[0].id)
        }
    }

    @Test
    fun `given transactions when filter type selected should filter by income or expense`() = runTest {
        viewModel.filteredTransactions.test {
            assertEquals(4, awaitItem().size)

            // 1 = Income
            viewModel.onFilterSelected(1)
            val incomeList = awaitItem()
            assertEquals(2, incomeList.size)
            assertTrue(incomeList.all { it.type == TransactionType.INCOME })

            // 2 = Expense
            viewModel.onFilterSelected(2)
            val expenseList = awaitItem()
            assertEquals(2, expenseList.size)
            assertTrue(expenseList.all { it.type == TransactionType.EXPENSE })

            // 0 = All
            viewModel.onFilterSelected(0)
            assertEquals(4, awaitItem().size)
        }
    }

    @Test
    fun `given query and filter when both applied should filter by both criteria`() = runTest {
        viewModel.filteredTransactions.test {
            assertEquals(4, awaitItem().size)

            viewModel.onFilterSelected(1) // Income only
            assertEquals(2, awaitItem().size)

            viewModel.onSearchQueryChanged("Freelance")
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Freelance UX", result[0].title)
        }
    }

    @Test
    fun `given action addTransaction when invoked should delegate to repository`() = runTest {
        viewModel.addTransaction(
            title = "Gasolina",
            amount = 45.0,
            type = TransactionType.EXPENSE,
            category = "Transporte"
        )

        coVerify(exactly = 1) {
            repository.addTransaction("Gasolina", 45.0, TransactionType.EXPENSE, "Transporte")
        }
    }

    @Test
    fun `given action deleteTransaction when invoked should delegate to repository`() = runTest {
        viewModel.deleteTransaction("tx-2")

        coVerify(exactly = 1) {
            repository.deleteTransaction("tx-2")
        }
    }
}
