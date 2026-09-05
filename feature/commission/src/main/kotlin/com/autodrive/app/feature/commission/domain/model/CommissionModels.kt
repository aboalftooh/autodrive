package com.autodrive.app.feature.commission.domain.model

import com.autodrive.app.core.model.money.Money

enum class CommissionStatus {
    WITHDRAWABLE,
    PENDING,
    PAID
}

data class CommissionEntry(
    val invoiceId: String,
    val invoiceNumber: Int,
    val amount: Money,
    val status: CommissionStatus,
    val createdAt: String,
    val paidAt: String? = null
)

data class CommissionSummary(
    val withdrawable: Money,
    val pending: Money,
    val paid: Money,
    val weeklyTotal: Money,
    val lastFriday9AmLabel: String,
    val weekStartMs: Long = 0L
)
