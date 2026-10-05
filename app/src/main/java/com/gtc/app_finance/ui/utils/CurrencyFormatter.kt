package com.gtc.app_finance.ui.utils

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {
    fun formatPesos(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        val isNegative = amount < 0
        val formatted = format.format(abs(amount))
        return if (isNegative) "- $ $formatted" else "$ $formatted"
    }

    fun formatPesosPositive(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        val formatted = format.format(abs(amount))
        return "$ $formatted"
    }
}
