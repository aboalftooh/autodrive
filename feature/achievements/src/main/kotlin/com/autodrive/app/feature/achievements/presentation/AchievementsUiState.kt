package com.autodrive.app.feature.achievements.presentation

import com.autodrive.app.core.model.money.Money
import java.math.BigDecimal

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val hasVerifiedCommissions: Boolean = false,
    val hasVerifiedBalance: Boolean = false,
    val lifetimeCommission: Money = Money.ZERO,
    val availableBalance: Money = Money.ZERO,
    val pendingCommission: Money = Money.ZERO,
    val joinedAtLabel: String = "",
    val hasActiveWithdrawal: Boolean = false,
    val weeklyPerformance: WeeklyPerformanceUiState = WeeklyPerformanceUiState(),
    val showTargetEditor: Boolean = false,
    val targetEditorInitialValue: Money? = null,
    val isSavingTarget: Boolean = false,
    val targetEditorError: String? = null,
)

data class WeeklyPerformanceUiState(
    val hasServerSnapshot: Boolean = false,
    val isRefreshing: Boolean = false,
    val weekStartMs: Long = 0L,
    val weekEndMs: Long = 0L,
    val currentAmount: Money = Money.ZERO,
    val currentCount: Long = 0L,
    val previousSamePeriodAmount: Money = Money.ZERO,
    val previousSamePeriodCount: Long = 0L,
    val changePercent: BigDecimal? = null,
    val trend: WeeklyPerformanceTrend = WeeklyPerformanceTrend.NO_BASELINE,
    val weeklyTarget: Money = Money.of(500_000L),
    val progressPercent: BigDecimal = BigDecimal.ZERO,
    val remainingToTarget: Money = Money.of(500_000L),
    val daysRemaining: Int = 0,
    val requiredDailyAverage: Money = Money.ZERO,
    val targetAchieved: Boolean = false,
    val targetAchievedEarly: Boolean = false,
    val targetSuggestionVisible: Boolean = false,
    val suggestedTarget: Money? = null,
    val loadError: Boolean = false,
)

enum class WeeklyPerformanceTrend {
    UP,
    DOWN,
    FLAT,
    UP_NO_BASELINE,
    NO_BASELINE,
}
