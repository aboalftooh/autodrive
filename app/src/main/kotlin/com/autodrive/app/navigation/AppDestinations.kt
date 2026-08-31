package com.autodrive.app.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    // ─── Auth & Onboarding ────────────────────
    data object Waiting         : Screen("waiting")
    data object PhoneInput      : Screen("phone_input")
    data object OtpInput        : Screen("otp_input/{phoneNumber}?devOtp={devOtp}") {
        fun createRoute(phoneNumber: String, devOtp: String? = null): String {
            val encodedPhone = URLEncoder.encode(phoneNumber, "UTF-8")
            return if (devOtp.isNullOrBlank()) {
                "otp_input/$encodedPhone"
            } else {
                "otp_input/$encodedPhone?devOtp=${URLEncoder.encode(devOtp, "UTF-8")}"
            }
        }
    }
    data object CodeInput       : Screen("code_input")
    data object SessionExpired  : Screen("session_expired")
    // ─── Registration ─────────────────────────
    data object AccountType     : Screen("account_type")
    data object BasicInfo       : Screen("basic_info/{accountType}") {
        fun createRoute(accountType: String) = "basic_info/$accountType"
    }
    data object Welcome         : Screen("welcome")
    // ─── Main App ─────────────────────────────
    data object Home            : Screen("home")
    data object Notifications   : Screen("notifications")
    data object RecentActivity  : Screen("recent_activity?newChat={newChat}") {
        fun createRoute(newChat: Boolean = false) =
            if (newChat) "recent_activity?newChat=true" else "recent_activity"
    }
    data object ActivityLog     : Screen("activity_log?filter={filter}") {
        fun createRoute(filter: String? = null) =
            if (filter != null) "activity_log?filter=$filter" else "activity_log"
    }
    data object Profile         : Screen("profile")
    data object Balance         : Screen("balance")
    data object CommissionReport : Screen("commission_report")
    data object InvoiceDetail   : Screen("invoice_detail/{invoiceId}") {
        fun createRoute(invoiceId: String) = "invoice_detail/$invoiceId"
    }
    data object WeeklyCompetition    : Screen("weekly_competition")
    data object InvoiceList          : Screen("invoice_list?weekMode={weekMode}") {
        fun createRoute(weekMode: String = "all") = "invoice_list?weekMode=$weekMode"
    }
    data object WinWeeks             : Screen("win_weeks")
    data object WeeklyCommissions    : Screen("weekly_commissions")
    data object CompetitionHistory   : Screen("competition_history")
    data object Chat            : Screen("chat/{conversationId}?title={title}") {
        fun createRoute(conversationId: String, title: String) =
            "chat/$conversationId?title=${URLEncoder.encode(title, "UTF-8")}"
    }
    // ─── Info & Legal ──────────────────────────
    data object AboutApp        : Screen("about_app")
    data object PrivacyPolicy   : Screen("privacy_policy")
    data object Faq             : Screen("faq")
}
