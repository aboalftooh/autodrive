package com.autodrive.app.navigation

import com.autodrive.app.feature.notifications.domain.model.NotificationType

internal object NotificationDestinationResolver {
    fun resolve(explicitRoute: String?, type: NotificationType?): String? {
        explicitRoute?.takeIf { it.isNotBlank() }?.let { return it }
        return when (type) {
            NotificationType.NEW_COMMISSION,
            NotificationType.COMMISSION_WITHDRAWABLE,
            NotificationType.COMMISSION_PAID,
            NotificationType.BALANCE_CREDITED,
            NotificationType.WITHDRAWAL_APPROVED,
            NotificationType.WITHDRAWAL_REJECTED,
            NotificationType.WITHDRAWAL_COMPLETED -> Screen.Balance.route

            NotificationType.WEEKLY_GOAL_ACHIEVED,
            NotificationType.WEEK_ENDING_SOON,
            NotificationType.INACTIVITY,
            NotificationType.NEW_INVOICE -> Screen.ActivityLog.createRoute()

            NotificationType.NEW_CHAT_MESSAGE -> Screen.RecentActivity.createRoute()
            NotificationType.PROFILE_INCOMPLETE -> Screen.Profile.route
            NotificationType.ADMIN_REMINDER -> Screen.Home.route
            NotificationType.WELCOME, null -> null
        }
    }
}
