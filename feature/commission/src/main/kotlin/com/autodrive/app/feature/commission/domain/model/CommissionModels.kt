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
    /** Full commission earned for the invoice. */
    val amount: Money,
    val status: CommissionStatus,
    val createdAt: String,
    val paidAt: String? = null,
    val paidOutAmount: Money = if (status == CommissionStatus.PAID) amount else Money.ZERO,
    val remainingAmount: Money = if (status == CommissionStatus.PAID) Money.ZERO else amount,
    val withdrawableAmount: Money = if (status == CommissionStatus.WITHDRAWABLE) remainingAmount else Money.ZERO,
    val pendingAmount: Money = if (status == CommissionStatus.PENDING) remainingAmount else Money.ZERO,
    val creditRemainingAmount: Money = Money.ZERO,
    val reasonCode: String? = null,
    val reasonMessage: String? = null,
)

data class CommissionSummary(
    val withdrawable: Money,
    val pending: Money,
    /** Amount already paid out, including partial payouts. */
    val paid: Money,
    val weeklyTotal: Money,
    val lastFriday9AmLabel: String,
    val weekStartMs: Long = 0L
)

enum class CommissionSnapshotSource {
    SERVER_CACHE,
    UNVERIFIED_EMPTY,
}

data class CommissionSnapshot(
    val summary: CommissionSummary,
    val entries: List<CommissionEntry>,
    val source: CommissionSnapshotSource,
    val lastSyncedAt: Long? = null,
)

data class CommissionPageCursor(
    val createdAt: String,
    val invoiceId: String,
)

data class CommissionPage(
    val entries: List<CommissionEntry>,
    val nextCursor: CommissionPageCursor?,
    val hasMore: Boolean,
)

data class CommissionListSummary(
    val totalCount: Long,
    val totalAmount: Money,
)
