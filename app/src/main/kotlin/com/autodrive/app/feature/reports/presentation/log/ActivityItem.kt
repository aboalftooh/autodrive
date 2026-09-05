package com.autodrive.app.feature.reports.presentation.log

import com.autodrive.app.feature.commission.domain.model.CommissionEntry
import com.autodrive.app.feature.commission.domain.model.CommissionStatus
import com.autodrive.app.feature.balance.domain.model.BalanceTransaction
import com.autodrive.app.feature.balance.domain.model.WithdrawalRequest
import com.autodrive.app.feature.balance.domain.model.WithdrawalStatus
import com.autodrive.app.core.model.money.Money

sealed class ActivityItem {
    abstract val sortTimeMs: Long
    abstract val title: String
    abstract val subtitle: String
    abstract val amount: Money
    abstract val emoji: String
    abstract val colorType: ActivityColorType

    data class Commission(
        val entry: CommissionEntry,
        override val sortTimeMs: Long
    ) : ActivityItem() {
        override val title    = "عمولة فاتورة #${entry.invoiceNumber}"
        override val subtitle = when (entry.status) {
            CommissionStatus.WITHDRAWABLE -> "قابلة للسحب"
            CommissionStatus.PENDING      -> "معلّقة"
            CommissionStatus.PAID         -> "مصروفة"
        }
        override val amount    = entry.amount
        override val emoji     = when (entry.status) {
            CommissionStatus.WITHDRAWABLE -> "💰"
            CommissionStatus.PENDING      -> "⏳"
            CommissionStatus.PAID         -> "✅"
        }
        override val colorType = when (entry.status) {
            CommissionStatus.WITHDRAWABLE -> ActivityColorType.GREEN
            CommissionStatus.PENDING      -> ActivityColorType.GOLD
            CommissionStatus.PAID         -> ActivityColorType.GRAY
        }
    }

    data class Withdrawal(
        val request: WithdrawalRequest,
        override val sortTimeMs: Long
    ) : ActivityItem() {
        override val title    = "طلب سحب"
        override val subtitle = when (request.status) {
            WithdrawalStatus.PENDING   -> "قيد المعالجة"
            WithdrawalStatus.APPROVED  -> "ناجح"
            WithdrawalStatus.REJECTED  -> "مرفوض"
            WithdrawalStatus.COMPLETED -> "مكتمل"
        }
        override val amount    = request.amount
        override val emoji     = when (request.status) {
            WithdrawalStatus.PENDING   -> "🏦"
            WithdrawalStatus.APPROVED  -> "💸"
            WithdrawalStatus.REJECTED  -> "❌"
            WithdrawalStatus.COMPLETED -> "💵"
        }
        override val colorType = when (request.status) {
            WithdrawalStatus.COMPLETED              -> ActivityColorType.GRAY
            WithdrawalStatus.APPROVED               -> ActivityColorType.RED
            WithdrawalStatus.PENDING                -> ActivityColorType.GOLD
            WithdrawalStatus.REJECTED               -> ActivityColorType.RED
        }
    }

    data class Transaction(
        val tx: BalanceTransaction,
        override val sortTimeMs: Long
    ) : ActivityItem() {
        override val title    = if (tx.type == "CREDIT") "إيداع في الرصيد" else "خصم من الرصيد"
        override val subtitle = tx.description.ifBlank { if (tx.type == "CREDIT") "رصيد مُضاف" else "رصيد مخصوم" }
        override val amount   = tx.amount
        override val emoji    = if (tx.type == "CREDIT") "⬆️" else "⬇️"
        override val colorType = if (tx.type == "CREDIT") ActivityColorType.GREEN else ActivityColorType.RED
    }
}

enum class ActivityColorType { GREEN, GOLD, GRAY, RED }
