package com.autodrive.app.feature.balance.domain.model

import com.autodrive.app.core.model.money.Money

enum class WithdrawalStatus(val label: String) {
    PENDING("قيد المعالجة"),
    APPROVED("ناجح"),
    REJECTED("مرفوض"),
    COMPLETED("مكتمل")
}

sealed class WithdrawalSubmitResult {
    data class Submitted(val serverId: String?) : WithdrawalSubmitResult()
    data object PendingLocal : WithdrawalSubmitResult()
}

data class MarketerBalance(
    val balance: Money,
    val pendingWithdrawal: Money,
    val updatedAt: String
)

data class BalanceTransaction(
    val id: String,
    val type: String,
    val amount: Money,
    val description: String,
    val createdAt: String
)

data class WithdrawalRequest(
    val id: String,
    val amount: Money,
    val status: WithdrawalStatus,
    val bankName: String,
    val bankAccount: String,
    val note: String?,
    val createdAt: String,
    val completedAt: String?,
    val transactionRef: String? = null
)
