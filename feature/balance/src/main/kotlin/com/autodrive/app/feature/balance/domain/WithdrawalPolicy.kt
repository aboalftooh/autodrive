package com.autodrive.app.feature.balance.domain

import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.balance.domain.model.WithdrawalRequest
import com.autodrive.app.feature.balance.domain.model.WithdrawalStatus
import javax.inject.Inject

class WithdrawalPolicy @Inject constructor() {
    fun isProcessing(status: WithdrawalStatus): Boolean =
        status == WithdrawalStatus.PENDING || status == WithdrawalStatus.APPROVED

    fun activeRequest(requests: List<WithdrawalRequest>): WithdrawalRequest? =
        requests
            .filter { isProcessing(it.status) }
            .maxByOrNull { it.createdAt }

    fun evaluate(
        amount: Money,
        balance: Money,
        requests: List<WithdrawalRequest>,
    ): WithdrawalDecision = when {
        activeRequest(requests) != null -> WithdrawalDecision.ActiveRequestExists
        !amount.isPositive() -> WithdrawalDecision.InvalidAmount
        amount > balance -> WithdrawalDecision.InsufficientBalance(balance)
        else -> WithdrawalDecision.Allowed
    }
}

sealed interface WithdrawalDecision {
    data object Allowed : WithdrawalDecision
    data object ActiveRequestExists : WithdrawalDecision
    data object InvalidAmount : WithdrawalDecision
    data class InsufficientBalance(val available: Money) : WithdrawalDecision
}
