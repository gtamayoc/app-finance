package com.gtc.app_finance

import com.gtc.app_finance.ui.utils.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun `formatPesos should format positive amounts with currency symbol`() {
        val formatted = CurrencyFormatter.formatPesos(1000000.0)
        assertTrue(formatted.startsWith("$"))
        assertTrue(formatted.contains("1.000.000") || formatted.contains("1,000,000"))
    }

    @Test
    fun `formatPesos should format zero amount`() {
        val formatted = CurrencyFormatter.formatPesos(0.0)
        assertEquals("$ 0", formatted)
    }

    @Test
    fun `formatPesos should format negative amounts with leading minus`() {
        val formatted = CurrencyFormatter.formatPesos(-50000.0)
        assertTrue(formatted.startsWith("- $"))
        assertTrue(formatted.contains("50.000") || formatted.contains("50,000"))
    }

    @Test
    fun `formatPesosPositive should format negative amounts as positive`() {
        val formatted = CurrencyFormatter.formatPesosPositive(-350000.0)
        assertTrue(formatted.startsWith("$"))
        assertTrue(!formatted.startsWith("-"))
        assertTrue(formatted.contains("350.000") || formatted.contains("350,000"))
    }

    @Test
    fun `formatPesos should format small decimals rounded`() {
        val formatted = CurrencyFormatter.formatPesos(1250.75)
        assertTrue(formatted.contains("1.251") || formatted.contains("1,251"))
    }
}
