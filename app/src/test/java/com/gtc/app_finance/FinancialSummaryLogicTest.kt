package com.gtc.app_finance

import com.gtc.app_finance.domain.model.Credit
import com.gtc.app_finance.domain.model.FinancialSummary
import com.gtc.app_finance.domain.model.Transaction
import com.gtc.app_finance.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialSummaryLogicTest {

    private fun calculateSummary(txList: List<Transaction>, crList: List<Credit>): FinancialSummary {
        var income = 0.0
        var expense = 0.0

        for (tx in txList) {
            if (tx.type == TransactionType.INCOME) {
                income += tx.amount
            } else {
                expense += tx.amount
            }
        }

        var debt = 0.0
        var activeCredits = 0
        for (cr in crList) {
            debt += cr.remainingAmount
            if (cr.remainingAmount > 0) {
                activeCredits++
            }
        }

        return FinancialSummary(
            totalBalance = income - expense,
            totalIncome = income,
            totalExpense = expense,
            totalDebt = debt,
            activeCreditsCount = activeCredits
        )
    }

    @Test
    fun `calculateSummary with empty data returns zeroed summary`() {
        val summary = calculateSummary(emptyList(), emptyList())
        assertEquals(0.0, summary.totalBalance, 0.001)
        assertEquals(0.0, summary.totalIncome, 0.001)
        assertEquals(0.0, summary.totalExpense, 0.001)
        assertEquals(0.0, summary.totalDebt, 0.001)
        assertEquals(0, summary.activeCreditsCount)
    }

    @Test
    fun `calculateSummary aggregates income and expense correctly`() {
        val txs = listOf(
            Transaction("1", "Sueldo", 3000000.0, TransactionType.INCOME, "Salario", "2026-10-01"),
            Transaction("2", "Bono", 500000.0, TransactionType.INCOME, "Salario", "2026-10-01"),
            Transaction("3", "Comida", 200000.0, TransactionType.EXPENSE, "Alimentación", "2026-10-02"),
            Transaction("4", "Transporte", 100000.0, TransactionType.EXPENSE, "Transporte", "2026-10-02")
        )
        val summary = calculateSummary(txs, emptyList())

        assertEquals(3500000.0, summary.totalIncome, 0.001)
        assertEquals(300000.0, summary.totalExpense, 0.001)
        assertEquals(3200000.0, summary.totalBalance, 0.001)
    }

    @Test
    fun `calculateSummary counts only active credits with remaining amount greater than zero`() {
        val credits = listOf(
            Credit("c1", "Credito 1", 1000000.0, 500000.0, "2026-12-01"),
            Credit("c2", "Credito 2", 800000.0, 0.0, "2026-10-01"),
            Credit("c3", "Credito 3", 2000000.0, 1500000.0, "2026-11-15")
        )
        val summary = calculateSummary(emptyList(), credits)

        assertEquals(2000000.0, summary.totalDebt, 0.001)
        assertEquals(2, summary.activeCreditsCount)
    }
}
