package com.autodrive.app.feature.achievements.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.core.sync.domain.SyncCoordinator
import com.autodrive.app.core.sync.domain.SyncReason
import com.autodrive.app.feature.balance.domain.WithdrawalPolicy
import com.autodrive.app.feature.balance.domain.usecase.ObserveBalanceUseCase
import com.autodrive.app.feature.balance.domain.usecase.ObserveWithdrawalRequestsUseCase
import com.autodrive.app.feature.commission.domain.model.CommissionSnapshotSource
import com.autodrive.app.feature.commission.domain.usecase.ObserveCommissionsUseCase
import com.autodrive.app.feature.profile.domain.usecase.ObserveProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    observeCommissions: ObserveCommissionsUseCase,
    observeBalance: ObserveBalanceUseCase,
    observeWithdrawals: ObserveWithdrawalRequestsUseCase,
    observeProfile: ObserveProfileUseCase,
    private val withdrawalPolicy: WithdrawalPolicy,
    private val syncCoordinator: SyncCoordinator,
) : ViewModel() {

    val state = combine(
        observeCommissions(),
        observeBalance(),
        observeWithdrawals(),
        observeProfile(),
    ) { commissions, balance, withdrawals, profile ->
        val hasVerifiedCommissions = commissions.source == CommissionSnapshotSource.SERVER_CACHE
        val hasVerifiedBalance = balance.updatedAt.isNotBlank()
        val summary = commissions.summary

        AchievementsUiState(
            isLoading = !hasVerifiedCommissions || profile == null,
            hasVerifiedCommissions = hasVerifiedCommissions,
            hasVerifiedBalance = hasVerifiedBalance,
            lifetimeCommission = Money.sum(
                listOf(summary.withdrawable, summary.pending, summary.paid),
            ),
            availableBalance = balance.balance,
            pendingCommission = summary.pending,
            joinedAtLabel = profile?.createdAt
                ?.let(FormatUtils::formatJoinDateShort)
                .orEmpty(),
            hasActiveWithdrawal = withdrawalPolicy.activeRequest(withdrawals) != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AchievementsUiState(),
    )

    init {
        viewModelScope.launch {
            syncCoordinator.requestSync(SyncReason.USER_REFRESH)
        }
    }
}
