package com.autodrive.app.feature.home.presentation

import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.commission.domain.model.CommissionSummary
import com.autodrive.app.feature.home.domain.model.AiInsight

enum class CommissionDataStatus {
    LOADING,
    FRESH,
    STALE,
    OFFLINE,
    ERROR,
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val refreshMessage: String? = null,
    val userName: String = "",
    val summary: CommissionSummary? = null,
    val syncedTotal: Money = Money.ZERO,
    val displayedTotal: Money = Money.ZERO,
    val isPumping: Boolean = false,
    val nextFriday9AmMs: Long = 0L,
    val weeklyTarget: Money = Money.of(500_000L),
    val commissionDataStatus: CommissionDataStatus = CommissionDataStatus.LOADING,
    val commissionLastUpdatedAt: Long? = null,
    val hasVerifiedCommissionData: Boolean = false,
    val unreadNotifications: Int = 0,
    val balance: Money = Money.ZERO,
    val balanceLoaded: Boolean = false,
    val insights: List<AiInsight> = emptyList(),
    val currentInsightIndex: Int = 0,
    val isInsightLoading: Boolean = false,
    val insightError: Boolean = false,
    val dynamoMessage: String = "",
)
