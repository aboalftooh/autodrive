package com.autodrive.app.core.platform.notifications

object AutoDriveNotificationConstants {
    const val EXTRA_NAV_ROUTE = "NAV_ROUTE"
    const val DATA_NAV_ROUTE  = "nav_route"
    const val DATA_TYPE       = "type"

    /**
     * يربط نوع الإشعار بوجهة التنقّل عند عدم وجود `nav_route` صريح.
     * مشترك بين خدمة FCM (المقدّمة) و MainActivity (النقر من إشعار النظام في الخلفية).
     */
    fun routeForType(type: String?): String? = when (type) {
        "NEW_CHAT_MESSAGE" -> "recent_activity"
        "NEW_COMMISSION", "COMMISSION_WITHDRAWABLE", "COMMISSION_PAID",
        "BALANCE_CREDITED", "WITHDRAWAL_APPROVED", "WITHDRAWAL_REJECTED",
        "WITHDRAWAL_COMPLETED" -> "balance"
        "NEW_INVOICE", "WEEKLY_GOAL_ACHIEVED", "WEEK_ENDING_SOON",
        "INACTIVITY"           -> "activity_log"
        "PROFILE_INCOMPLETE"   -> "profile"
        else                   -> null
    }
}
