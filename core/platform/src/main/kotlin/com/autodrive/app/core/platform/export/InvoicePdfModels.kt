package com.autodrive.app.core.platform.export

import com.autodrive.app.core.model.money.Money

data class InvoicePdfEntry(
    val invoiceNumber: Int,
    val createdAt: String,
    val amount: Money,
)

data class InvoicePdfItem(
    val itemName: String,
    val quantity: Int,
    val sellPrice: Money,
    val totalPrice: Money,
)
