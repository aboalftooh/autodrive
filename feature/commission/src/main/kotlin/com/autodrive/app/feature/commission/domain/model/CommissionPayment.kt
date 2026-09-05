package com.autodrive.app.feature.commission.domain.model

import com.autodrive.app.core.model.money.Money

data class CommissionPayment(
    val id: String,
    val clientId: String,
    val amount: Money,
    val note: String?,
    val invoiceIds: List<String>,
    val createdAt: String
)
