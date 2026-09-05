package com.autodrive.app.feature.notifications.domain.model

data class AppNotification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val createdAt: String,
    val navRoute: String? = null
)

enum class NotificationType(val key: String) {
    NEW_COMMISSION("NEW_COMMISSION"),
    COMMISSION_WITHDRAWABLE("COMMISSION_WITHDRAWABLE"),
    COMMISSION_PAID("COMMISSION_PAID"),
    BALANCE_CREDITED("BALANCE_CREDITED"),
    NEW_INVOICE("NEW_INVOICE"),
    WEEK_ENDING_SOON("WEEK_ENDING_SOON"),
    INACTIVITY("INACTIVITY"),
    WEEKLY_GOAL_ACHIEVED("WEEKLY_GOAL_ACHIEVED"),
    NEW_CHAT_MESSAGE("NEW_CHAT_MESSAGE"),
    WITHDRAWAL_APPROVED("WITHDRAWAL_APPROVED"),
    WITHDRAWAL_REJECTED("WITHDRAWAL_REJECTED"),
    WITHDRAWAL_COMPLETED("WITHDRAWAL_COMPLETED"),
    PROFILE_INCOMPLETE("PROFILE_INCOMPLETE"),
    WELCOME("WELCOME"),
    ADMIN_REMINDER("ADMIN_REMINDER");

    companion object {
        fun from(key: String): NotificationType? = entries.find { it.key == key }
    }
}

fun AppNotification.notificationType(): NotificationType? = NotificationType.from(type)
