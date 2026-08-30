package com.autodrive.app.feature.commission.domain.model

import com.autodrive.app.core.model.money.Money

data class InvoiceItem(
    val id: String,
    val itemName: String,
    val itemType: String,
    val description: String?,
    val quantity: Int,
    val sellPrice: Money,
    val totalPrice: Money
)

data class Invoice(
    val id: String,
    val clientId: String,
    val commission: Money,
    val status: InvoiceStatus,
    val category: InvoiceCategory,
    val totalAmount: Money,
    val invoiceNumber: Int,
    val createdAt: String
)

enum class InvoiceStatus { CLOSED_CASH, CLOSED_CREDIT, OPEN, CANCELLED }
enum class InvoiceCategory { SALE, SERVICE, OTHER }
