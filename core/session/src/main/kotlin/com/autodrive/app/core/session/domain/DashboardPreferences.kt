package com.autodrive.app.core.session.domain

import com.autodrive.app.core.model.money.Money

interface DashboardPreferences {
    var weeklyTarget: Money
    var lastDisplayedTotal: Money
    var lastDisplayedWeekStartMs: Long
}
