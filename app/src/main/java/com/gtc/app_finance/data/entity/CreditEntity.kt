package com.gtc.app_finance.data.entity

import com.gtc.app_finance.domain.model.Credit

data class CreditEntity(
    val id: String,
    val title: String,
    val totalAmount: Double,
    val remainingAmount: Double,
    val dueDate: String
) {
    fun toDomain(): Credit {
        return Credit(
            id = id,
            title = title,
            totalAmount = totalAmount,
            remainingAmount = remainingAmount,
            dueDate = dueDate
        )
    }

    companion object {
        fun fromDomain(credit: Credit): CreditEntity {
            return CreditEntity(
                id = credit.id,
                title = credit.title,
                totalAmount = credit.totalAmount,
                remainingAmount = credit.remainingAmount,
                dueDate = credit.dueDate
            )
        }
    }
}
