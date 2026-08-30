package com.autodrive.app.feature.notifications.presentation

import com.autodrive.app.feature.notifications.domain.model.AppNotification
import com.autodrive.app.feature.notifications.domain.model.NotificationType
import com.autodrive.app.feature.notifications.domain.model.notificationType

fun AppNotification.icon(): String = when (notificationType()) {
    NotificationType.NEW_COMMISSION -> "💰"
    NotificationType.COMMISSION_WITHDRAWABLE -> "🟢"
    NotificationType.COMMISSION_PAID -> "💵"
    NotificationType.BALANCE_CREDITED -> "🏦"
    NotificationType.NEW_INVOICE -> "🧾"
    NotificationType.WEEK_ENDING_SOON -> "⏰"
    NotificationType.INACTIVITY -> "😴"
    NotificationType.WEEKLY_GOAL_ACHIEVED -> "🏆"
    NotificationType.NEW_CHAT_MESSAGE -> "💬"
    NotificationType.WITHDRAWAL_APPROVED -> "✅"
    NotificationType.WITHDRAWAL_REJECTED -> "❌"
    NotificationType.WITHDRAWAL_COMPLETED -> "💸"
    NotificationType.PROFILE_INCOMPLETE -> "⚠️"
    NotificationType.WELCOME -> "👋"
    NotificationType.ADMIN_REMINDER -> "📢"
    null -> "🔔"
}
