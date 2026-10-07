package com.gtc.app_finance.data.entity

import com.gtc.app_finance.domain.model.Transaction
import com.gtc.app_finance.domain.model.TransactionType

data class TransactionEntity(
    val id: String,
    val title: String,
    val amount: Double,
    val type: String, // 'income' | 'expense'
    val category: String,
    val date: String,
    val paymentId: String? = null
) {
    fun toDomain(): Transaction {
        return Transaction(
            id = id,
            title = title,
            amount = amount,
            type = if (type.lowercase() == "income") TransactionType.INCOME else TransactionType.EXPENSE,
            category = category,
            date = date,
            paymentId = paymentId
        )
    }

    companion object {
        fun fromDomain(transaction: Transaction): TransactionEntity {
            return TransactionEntity(
                id = transaction.id,
                title = transaction.title,
                amount = transaction.amount,
                type = if (transaction.type == TransactionType.INCOME) "income" else "expense",
                category = transaction.category,
                date = transaction.date,
                paymentId = transaction.paymentId
            )
        }
    }
}
