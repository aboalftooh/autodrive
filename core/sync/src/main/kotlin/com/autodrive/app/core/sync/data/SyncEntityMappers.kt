package com.autodrive.app.core.sync.data

import com.autodrive.app.core.database.entities.AutoDriveUserEntity
import com.autodrive.app.core.database.entities.BalanceTransactionEntity
import com.autodrive.app.core.database.entities.ChatMessageEntity
import com.autodrive.app.core.database.entities.CommissionPaymentEntity
import com.autodrive.app.core.database.entities.ConversationEntity
import com.autodrive.app.core.database.entities.InvoiceEntity
import com.autodrive.app.core.database.entities.MarketerBalanceEntity
import com.autodrive.app.core.database.entities.NotificationEntity
import com.autodrive.app.core.database.entities.PaymentEntity
import com.autodrive.app.core.database.entities.WithdrawalRequestEntity
import com.autodrive.app.core.network.dto.AutoDriveUserDto
import com.autodrive.app.core.network.dto.BalanceTransactionDto
import com.autodrive.app.core.network.dto.chat.ChatMessageDto
import com.autodrive.app.core.network.dto.CommissionPaymentDto
import com.autodrive.app.core.network.dto.chat.ConversationDto
import com.autodrive.app.core.network.dto.InvoiceDto
import com.autodrive.app.core.network.dto.MarketerBalanceDto
import com.autodrive.app.core.network.dto.NotificationDto
import com.autodrive.app.core.network.dto.PaymentDto
import com.autodrive.app.core.network.dto.WithdrawalRequestDto
import java.time.OffsetDateTime
import java.math.BigDecimal

internal fun AutoDriveUserDto.toEntity() = AutoDriveUserEntity(
    id = id, userId = userId, clientId = clientId, orgId = orgId,
    accountType = accountType, fullName = fullName, phone = phone,
    bankName = bankName, bankAccount = bankAccount, workshopName = workshopName,
    specialty = specialty, workersCount = workersCount, address = address,
    createdAt = createdAt, updatedAt = updatedAt
)

internal fun InvoiceDto.toEntity() = InvoiceEntity(
    id = id, clientId = clientId, commission = commission,
    status = status, category = category, totalAmount = totalAmount,
    invoiceNumber = invoiceNumber, createdAt = createdAt
)

internal fun PaymentDto.toEntity() = PaymentEntity(
    id = id, clientId = clientId, invoiceId = invoiceId, amount = amount, createdAt = createdAt
)

internal fun CommissionPaymentDto.toEntity() = CommissionPaymentEntity(
    id = id, clientId = clientId, amount = amount, note = note,
    invoiceIds = invoiceIds, createdAt = createdAt
)

internal fun MarketerBalanceDto.toEntity(userId: String) = MarketerBalanceEntity(
    id = id,
    userId = userId,
    clientId = clientId,
    balance = balance,
    pendingWithdrawal = BigDecimal.ZERO,
    updatedAt = updatedAt
)

internal fun BalanceTransactionDto.toEntity(userId: String) = BalanceTransactionEntity(
    id = id,
    userId = userId,
    clientId = clientId,
    type = type,
    amount = amount,
    description = note?.ifBlank { null } ?: referenceType,
    createdAt = createdAt
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
    completedAt = processedAt
)

internal fun ChatMessageDto.toChatEntity(convId: String) = ChatMessageEntity(
    id = id,
    conversationId = convId,
    senderId = senderId,
    senderType = senderType,
    content = body,
    type = type.ifBlank { "TEXT" },
    isRead = isRead,
    createdAt = OffsetDateTime.parse(createdAt).toInstant().toEpochMilli(),
    status = "SENT",
    mediaUrl = mediaUrl,
    mediaMime = mediaMime,
    mediaDurationMs = mediaDurationMs,
)

internal fun NotificationDto.toEntity() = NotificationEntity(
    id = id, userId = userId, clientId = clientId,
    type = type, title = title, body = body,
    isRead = isRead, createdAt = createdAt
)

internal fun ConversationDto.toEntity(userId: String) = ConversationEntity(
    id = id,
    marketerId = userId,
    clientId = clientId,
    subject = subject,
    title = subject.ifBlank { "الإدارة" },
    createdAt = runCatching {
        OffsetDateTime.parse(createdAt).toInstant().toEpochMilli()
    }.getOrDefault(System.currentTimeMillis())
)
