package com.autodrive.app.feature.home.presentation

import com.autodrive.app.feature.commission.domain.model.CommissionSummary
import com.autodrive.app.feature.home.domain.model.AiInsight

import com.autodrive.app.core.model.money.Money

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val userName: String = "",
    val summary: CommissionSummary? = null,
    val syncedTotal: Money = Money.ZERO,
    val displayedTotal: Money = Money.ZERO,
    val isPumping: Boolean = false,
    val nextFriday9AmMs: Long = 0L,
    val weeklyTarget: Money = Money.of(500_000L),
    val unreadNotifications: Int = 0,
    val balance: Money = Money.ZERO,
    val balanceLoaded: Boolean = false,
    // AI Insight
    val insights: List<AiInsight> = emptyList(),
    val currentInsightIndex: Int = 0,
    val isInsightLoading: Boolean = false,
    val insightError: Boolean = false,
    // عم دينمو — الرسالة من Supabase فقط
    val dynamoMessage: String = "",
)
