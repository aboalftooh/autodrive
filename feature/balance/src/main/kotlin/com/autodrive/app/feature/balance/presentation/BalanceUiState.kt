package com.autodrive.app.feature.balance.presentation

import com.autodrive.app.feature.balance.domain.model.BalanceActivityPageCursor
import com.autodrive.app.feature.balance.domain.model.BalanceTransaction
import com.autodrive.app.feature.balance.domain.model.MarketerBalance
import com.autodrive.app.feature.balance.domain.model.WithdrawalRequest

data class BalanceUiState(
    val isLoading: Boolean = true,
    val balance: MarketerBalance? = null,
    val withdrawalRequests: List<WithdrawalRequest> = emptyList(),
    val activeWithdrawal: WithdrawalRequest? = null,
    val transactions: List<BalanceTransaction> = emptyList(),
    val nextActivityCursor: BalanceActivityPageCursor? = null,
    val hasMoreActivities: Boolean = false,
    val isLoadingActivities: Boolean = true,
    val isLoadingMoreActivities: Boolean = false,
    val activityError: String? = null,
    val errorMessage: String? = null,
    val showWithdrawSheet: Boolean = false,
    val isRefreshingBalance: Boolean = false,
    val withdrawalAmount: String = "",
    val withdrawalNote: String = "",
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val submitPendingLocal: Boolean = false,
    val submitError: String? = null,
)
