package com.autodrive.app.feature.reports.presentation.log

import com.autodrive.app.core.model.money.Money

enum class ReportsLoadState {
    LOADING,
    CONTENT,
    ERROR
}

enum class TrendDirection {
    UP,
    DOWN,
    FLAT,
    NEW
}

data class TrendComparison(
    val direction: TrendDirection,
    val percent: Int? = null
)

data class ReportsUiState(
    val loadState: ReportsLoadState = ReportsLoadState.LOADING,
    val errorMessage: String? = null,
    val joinDate: String = "",
    val currentWeekLabel: String = "",
    val currentWeekPurchases: Money = Money.ZERO,
    val previousWeekPurchases: Money = Money.ZERO,
    val purchaseTrend: TrendComparison = TrendComparison(TrendDirection.FLAT, 0),
    val currentWeekCommissions: Money = Money.ZERO,
    val previousWeekCommissions: Money = Money.ZERO,
    val commissionTrend: TrendComparison = TrendComparison(TrendDirection.FLAT, 0),
    val currentWeekInvoiceCount: Int = 0,
    val previousWeekInvoiceCount: Int = 0,
    val balance: Money = Money.ZERO,
    val pending: Money = Money.ZERO,
    val lifetimeCommissions: Money = Money.ZERO,
    val winCount: Int? = null
)
