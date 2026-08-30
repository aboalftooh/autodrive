package com.autodrive.app.feature.commission.domain.model

import com.autodrive.app.core.model.money.Money

data class Payment(
    val id: String,
    val invoiceId: String,
    val amount: Money,
    val createdAt: String
)
