package com.autodrive.app.feature.commission.data

import com.autodrive.app.core.database.entities.CommissionPaymentEntity
import com.autodrive.app.core.database.entities.InvoiceEntity
import com.autodrive.app.core.database.entities.PaymentEntity
import com.autodrive.app.core.network.dto.CommissionPaymentDto
import com.autodrive.app.core.network.dto.InvoiceDto
import com.autodrive.app.core.network.dto.PaymentDto

internal fun InvoiceDto.toEntity() = InvoiceEntity(
    id = id,
    clientId = clientId,
    commission = commission,
    status = status,
    category = category,
    totalAmount = totalAmount,
    invoiceNumber = invoiceNumber,
    createdAt = createdAt,
)

internal fun PaymentDto.toEntity() = PaymentEntity(
    id = id,
    clientId = clientId,
    invoiceId = invoiceId,
    amount = amount,
    createdAt = createdAt,
)

internal fun CommissionPaymentDto.toEntity() = CommissionPaymentEntity(
    id = id,
    clientId = clientId,
    amount = amount,
    note = note,
    invoiceIds = invoiceIds,
    createdAt = createdAt,
)
