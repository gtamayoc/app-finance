package com.gtc.app_finance

import app.cash.turbine.test
import com.gtc.app_finance.data.repository.FinanceRepository
import com.gtc.app_finance.domain.model.Credit
import com.gtc.app_finance.ui.screens.credits.CreditsViewModel
import com.gtc.app_finance.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CreditsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: FinanceRepository = mockk(relaxed = true)
    private val creditsFlow = MutableStateFlow<List<Credit>>(emptyList())

    private lateinit var viewModel: CreditsViewModel

    private val sampleCredits = listOf(
        Credit(id = "c-1", title = "Tarjeta Visa", totalAmount = 1000.0, remainingAmount = 800.0, dueDate = "2026-11-01"),
        Credit(id = "c-2", title = "Préstamo Personal", totalAmount = 5000.0, remainingAmount = 3500.0, dueDate = "2026-12-15")
    )

    @Before
    fun setUp() {
        every { repository.credits } returns creditsFlow
        creditsFlow.value = sampleCredits
        viewModel = CreditsViewModel(repository)
    }

    @Test
    fun `given credits in repository when credits observed should emit list`() = runTest {
        viewModel.credits.test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertEquals("Tarjeta Visa", items[0].title)
            assertEquals(800.0, items[0].remainingAmount, 0.001)
        }
    }

    @Test
    fun `given action addCredit when invoked should delegate to repository`() = runTest {
        viewModel.addCredit("Préstamo Moto", 2500.0, "2026-10-30")

        coVerify(exactly = 1) {
            repository.addCredit("Préstamo Moto", 2500.0, "2026-10-30")
        }
    }

    @Test
    fun `given action recordPayment when invoked should delegate to repository`() = runTest {
        viewModel.recordPayment("c-1", 150.0)

        coVerify(exactly = 1) {
            repository.recordPayment("c-1", 150.0)
        }
    }

    @Test
    fun `given action deleteCredit when invoked should delegate to repository`() = runTest {
        viewModel.deleteCredit("c-2")

        coVerify(exactly = 1) {
            repository.deleteCredit("c-2")
        }
    }
}
