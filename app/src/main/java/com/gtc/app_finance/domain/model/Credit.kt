package com.gtc.app_finance.domain.model

data class Credit(
    val id: String,
    val title: String,
    val totalAmount: Double,
    val remainingAmount: Double,
    val dueDate: String
) {
    val progress: Float
        get() = if (totalAmount > 0) ((totalAmount - remainingAmount) / totalAmount).toFloat().coerceIn(0f, 1f) else 1f

    val isPaid: Boolean
        get() = remainingAmount <= 0.0
}
