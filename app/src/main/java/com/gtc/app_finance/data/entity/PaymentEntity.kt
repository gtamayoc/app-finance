package com.gtc.app_finance.data.entity

import com.gtc.app_finance.domain.model.Payment

data class PaymentEntity(
    val id: String,
    val creditId: String,
    val amount: Double,
    val date: String
) {
    fun toDomain(): Payment {
        return Payment(
            id = id,
            creditId = creditId,
            amount = amount,
            date = date
        )
    }

    companion object {
        fun fromDomain(payment: Payment): PaymentEntity {
            return PaymentEntity(
                id = payment.id,
                creditId = payment.creditId,
                amount = payment.amount,
                date = payment.date
            )
        }
    }
}
