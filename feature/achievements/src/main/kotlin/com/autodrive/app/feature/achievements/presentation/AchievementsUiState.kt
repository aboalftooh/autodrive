package com.autodrive.app.feature.achievements.presentation

import com.autodrive.app.core.model.money.Money

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val hasVerifiedCommissions: Boolean = false,
    val hasVerifiedBalance: Boolean = false,
    val lifetimeCommission: Money = Money.ZERO,
    val availableBalance: Money = Money.ZERO,
    val pendingCommission: Money = Money.ZERO,
    val joinedAtLabel: String = "",
    val hasActiveWithdrawal: Boolean = false,
)
