package com.autodrive.app.navigation

import com.autodrive.app.feature.notifications.domain.model.NotificationType

internal object NotificationDestinationResolver {
    fun resolve(explicitRoute: String?, type: NotificationType?): String? = when (type) {
        NotificationType.NEW_COMMISSION,
        NotificationType.COMMISSION_WITHDRAWABLE,
        NotificationType.COMMISSION_PAID,
        NotificationType.BALANCE_CREDITED,
        NotificationType.WITHDRAWAL_APPROVED,
        NotificationType.WITHDRAWAL_REJECTED,
        NotificationType.WITHDRAWAL_COMPLETED -> Screen.Balance.route

        NotificationType.NEW_INVOICE -> Screen.InvoiceList.createRoute()

        NotificationType.WEEKLY_GOAL_ACHIEVED,
        NotificationType.WEEK_ENDING_SOON,
        NotificationType.INACTIVITY -> Screen.Home.route

        NotificationType.NEW_CHAT_MESSAGE -> Screen.RecentActivity.createRoute()
        NotificationType.PROFILE_INCOMPLETE -> Screen.Profile.route
        NotificationType.ADMIN_REMINDER -> Screen.Home.route
        NotificationType.WELCOME, null -> explicitRoute?.takeIf { it.isNotBlank() }
    }
}
