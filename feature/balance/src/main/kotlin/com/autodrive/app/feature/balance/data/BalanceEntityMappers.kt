package com.autodrive.app.feature.balance.data

import com.autodrive.app.core.database.entities.BalanceTransactionEntity
import com.autodrive.app.core.database.entities.MarketerBalanceEntity
import com.autodrive.app.core.database.entities.WithdrawalRequestEntity
import com.autodrive.app.core.network.dto.BalanceTransactionDto
import com.autodrive.app.core.network.dto.MarketerBalanceDto
import com.autodrive.app.core.network.dto.WithdrawalRequestDto
import java.math.BigDecimal

internal fun MarketerBalanceDto.toEntity(userId: String) = MarketerBalanceEntity(
    id = id,
    userId = userId,
    clientId = clientId,
    balance = balance,
    pendingWithdrawal = BigDecimal.ZERO,
    updatedAt = updatedAt,
)

internal fun BalanceTransactionDto.toEntity(userId: String) = BalanceTransactionEntity(
    id = id,
    userId = userId,
    clientId = clientId,
    type = type,
    amount = amount,
    description = note?.ifBlank { null } ?: referenceType,
    createdAt = createdAt,
)

internal fun WithdrawalRequestDto.toEntity(userId: String) = WithdrawalRequestEntity(
    id = id,
    userId = userId,
    clientId = clientId,
    amount = amount,
    status = status,
    bankName = bankName,
    bankAccount = bankAccount,
    transactionRef = transactionRef,
    note = note,
    createdAt = requestedAt,
    completedAt = processedAt,
)
