package com.autodrive.app.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.autodrive.app.feature.notifications.domain.model.notificationType
import com.autodrive.app.feature.auth.presentation.join.CodeInputScreen
import com.autodrive.app.feature.auth.presentation.join.WelcomeScreen
import com.autodrive.app.feature.auth.presentation.login.OtpInputScreen
import com.autodrive.app.feature.auth.presentation.login.PhoneAuthViewModel
import com.autodrive.app.feature.auth.presentation.login.PhoneInputScreen
import com.autodrive.app.feature.auth.presentation.login.SessionExpiredScreen
import com.autodrive.app.feature.auth.presentation.register.BasicInfoScreen
import com.autodrive.app.feature.balance.presentation.BalanceScreen
import com.autodrive.app.feature.chat.presentation.ChatScreen
import com.autodrive.app.feature.commission.presentation.CommissionReportScreen
import com.autodrive.app.feature.competition.domain.model.CompetitionAvailability
import com.autodrive.app.feature.competition.presentation.WeeklyCompetitionScreen
import com.autodrive.app.feature.home.presentation.HomeScreen
import com.autodrive.app.feature.notifications.presentation.NotificationsScreen
import com.autodrive.app.feature.profile.presentation.ProfileScreen
import com.autodrive.app.feature.reports.presentation.log.ActivityLogScreen
import com.autodrive.app.feature.reports.presentation.log.CompetitionHistoryScreen
import com.autodrive.app.feature.reports.presentation.log.InvoiceDetailScreen
import com.autodrive.app.feature.reports.presentation.log.InvoiceListScreen
import com.autodrive.app.feature.reports.presentation.log.WeeklyCommissionsScreen
import com.autodrive.app.feature.reports.presentation.log.WinWeeksScreen
import com.autodrive.app.feature.reports.presentation.recent.RecentActivityScreen
import com.autodrive.app.feature.info.presentation.AboutAppScreen
import com.autodrive.app.feature.info.presentation.FaqScreen
import com.autodrive.app.feature.info.presentation.PrivacyPolicyScreen
import java.net.URLDecoder

internal fun NavGraphBuilder.authAndRegistrationGraph(
    navController: NavHostController,
    navVm: AppNavigationViewModel,
) {
composable(Screen.SessionExpired.route) {
    SessionExpiredScreen(
        userName = navVm.userName,
        onLogin = {
            navController.navigate(Screen.PhoneInput.route) {
                popUpTo(Screen.SessionExpired.route) { inclusive = true }
            }
        },
        onRetry = {
            navController.navigate(Screen.PhoneInput.route) {
                popUpTo(Screen.SessionExpired.route) { inclusive = true }
            }
        }
    )
}

composable(Screen.CodeInput.route) {
    CodeInputScreen(
        onOtpReady = { phone, devOtp ->
            navController.navigate(Screen.OtpInput.createRoute(phone, devOtp)) { launchSingleTop = true }
        },
        onBack = { navController.popBackStack() }
    )
}

composable(Screen.PhoneInput.route) {
    val phoneAuthViewModel: PhoneAuthViewModel = hiltViewModel()
    PhoneInputScreen(
        onBack = { navController.popBackStack() },
        onOtpSent = { phoneNumber, devOtp ->
            navController.navigate(Screen.OtpInput.createRoute(phoneNumber, devOtp)) {
                launchSingleTop = true
            }
            phoneAuthViewModel.resetToIdle()
        },
        onJoinCodeRequired = {
            navController.navigate(Screen.CodeInput.route) { launchSingleTop = true }
            phoneAuthViewModel.resetToIdle()
        },
        viewModel = phoneAuthViewModel
    )
}

composable(
    route = Screen.OtpInput.route,
    arguments = listOf(
        navArgument("phoneNumber") { type = NavType.StringType },
        navArgument("devOtp") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }
    )
) { backStackEntry ->
    val phoneNumber = backStackEntry.arguments
        ?.getString("phoneNumber")
        ?.let { URLDecoder.decode(it, "UTF-8") }
        .orEmpty()
    val devOtp = backStackEntry.arguments
        ?.getString("devOtp")
        ?.let { URLDecoder.decode(it, "UTF-8") }
    OtpInputScreen(
        phoneNumber = phoneNumber,
        devOtp = devOtp,
        onVerified = {
            val destination = if (navVm.isRegistrationComplete) {
                Screen.Home.route
            } else {
                Screen.BasicInfo.createRoute(navVm.accountType.ifBlank { "MARKETER" })
            }
            navController.navigate(destination) {
                popUpTo(Screen.PhoneInput.route) { inclusive = true }
            }
        },
        onBack = { navController.popBackStack() }
    )
}

composable(Screen.BasicInfo.route) { backStack ->
    val accountType = backStack.arguments?.getString("accountType") ?: "MARKETER"
    BasicInfoScreen(
        accountType = accountType,
        onCompleted = {
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        },
        onBack = { navController.popBackStack() }
    )
}

composable(Screen.Welcome.route) {
    WelcomeScreen(
        onContinue = {
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    )
}

}

internal fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    onOpenNewChat: () -> Unit,
    unreadMessages: Int,
    competitionAvailability: CompetitionAvailability,
    onRefreshCompetitionAvailability: () -> Unit,
) {
composable(Screen.Home.route) {
    LaunchedEffect(Unit) { onRefreshCompetitionAvailability() }
    HomeScreen(
        onNavigateRecent = { navController.navigate(Screen.RecentActivity.createRoute()) },
        onNavigateLog = { filter -> navController.navigate(Screen.ActivityLog.createRoute(filter)) },
        onNavigateProfile = { navController.navigate(Screen.Profile.route) },
        onNavigateNotifications = { navController.navigate(Screen.Notifications.route) },
        onNavigateCompetition = { navController.navigate(Screen.WeeklyCompetition.route) },
        competitionAvailability = competitionAvailability,
        onAddClick = onOpenNewChat,
        unreadMessages = unreadMessages,
    )
}

composable(Screen.WeeklyCompetition.route) {
    WeeklyCompetitionScreen(
        availability = competitionAvailability,
        onBack = { navController.popBackStack() },
        onNavigateCompetitionHistory = { navController.navigate(Screen.CompetitionHistory.route) },
        onNavigateWinWeeks = { navController.navigate(Screen.WinWeeks.route) },
    )
}

composable(Screen.Balance.route) {
    BalanceScreen(
        onBack = { navController.popBackStack() },
        onOpenReport = { navController.navigate(Screen.CommissionReport.route) }
    )
}

composable(Screen.CommissionReport.route) {
    CommissionReportScreen(onBack = { navController.popBackStack() })
}

composable(
    route = Screen.InvoiceDetail.route,
    arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
) { backStack ->
    val invoiceId = backStack.arguments?.getString("invoiceId") ?: ""
    InvoiceDetailScreen(
        invoiceId = invoiceId,
        onBack = { navController.popBackStack() }
    )
}

composable(Screen.Notifications.route) {
    NotificationsScreen(
        onBack = { navController.popBackStack() },
        onNotificationClick = { notification ->
            NotificationDestinationResolver.resolve(
                explicitRoute = notification.navRoute,
                type = notification.notificationType(),
            )?.let { route -> navController.navigate(route) }
        }
    )
}

composable(
    route = Screen.RecentActivity.route,
    arguments = listOf(
        navArgument("newChat") {
            type = NavType.BoolType
            defaultValue = false
        }
    )
) { backStack ->
    val newChat = backStack.arguments?.getBoolean("newChat") ?: false
    RecentActivityScreen(
        onNavigateHome = { navController.navigate(Screen.Home.route) },
        onNavigateLog = { navController.navigate(Screen.ActivityLog.createRoute()) },
        onNavigateProfile = { navController.navigate(Screen.Profile.route) },
        onAddClick = onOpenNewChat,
        onOpenConversation = { id, title ->
            navController.navigate(Screen.Chat.createRoute(id, title))
        },
        autoStartNewChat = newChat,
        unreadMessages = unreadMessages,
    )
}

composable(
    route = Screen.ActivityLog.route,
    arguments = listOf(
        navArgument("filter") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }
    )
) {
    ActivityLogScreen(
        onNavigateHome = { navController.navigate(Screen.Home.route) },
        onNavigateRecent = { navController.navigate(Screen.RecentActivity.createRoute()) },
        onNavigateProfile = { navController.navigate(Screen.Profile.route) },
        onNavigateBalance = { navController.navigate(Screen.Balance.route) },
        onNavigateInvoiceDetail = { id -> navController.navigate(Screen.InvoiceDetail.createRoute(id)) },
        onNavigateInvoiceList = { weekMode -> navController.navigate(Screen.InvoiceList.createRoute(weekMode)) },
        onNavigateWinWeeks = { navController.navigate(Screen.WinWeeks.route) },
        onNavigateWeeklyCommissions = { navController.navigate(Screen.WeeklyCommissions.route) },
        onNavigateCompetitionHistory = { navController.navigate(Screen.CompetitionHistory.route) },
        onAddClick = onOpenNewChat,
        unreadMessages = unreadMessages,
        competitionAvailability = competitionAvailability,
    )
}

composable(
    route = Screen.InvoiceList.route,
    arguments = listOf(
        navArgument("weekMode") {
            type = NavType.StringType
            nullable = true
            defaultValue = "all"
        }
    )
) { backStack ->
    val weekMode = backStack.arguments?.getString("weekMode") ?: "all"
    InvoiceListScreen(
        weekMode = weekMode,
        onBack = { navController.popBackStack() },
        onNavigateInvoiceDetail = { id -> navController.navigate(Screen.InvoiceDetail.createRoute(id)) }
    )
}

composable(Screen.WinWeeks.route) {
    if (competitionAvailability == CompetitionAvailability.ACTIVE) {
        WinWeeksScreen(onBack = { navController.popBackStack() })
    } else {
        WeeklyCompetitionScreen(
            availability = competitionAvailability,
            onBack = { navController.popBackStack() },
        )
    }
}

composable(Screen.WeeklyCommissions.route) {
    WeeklyCommissionsScreen(onBack = { navController.popBackStack() })
}

composable(Screen.CompetitionHistory.route) {
    if (competitionAvailability == CompetitionAvailability.ACTIVE) {
        CompetitionHistoryScreen(onBack = { navController.popBackStack() })
    } else {
        WeeklyCompetitionScreen(
            availability = competitionAvailability,
            onBack = { navController.popBackStack() },
        )
    }
}

composable(Screen.Profile.route) {
    ProfileScreen(
        onNavigateHome = { navController.navigate(Screen.Home.route) },
        onNavigateRecent = { navController.navigate(Screen.RecentActivity.createRoute()) },
        onNavigateLog = { navController.navigate(Screen.ActivityLog.createRoute()) },
        onSignedOut = {
            navController.navigate(Screen.PhoneInput.route) {
                popUpTo(0) { inclusive = true }
            }
        },
        onAddClick = onOpenNewChat,
        onNavigateAbout = { navController.navigate(Screen.AboutApp.route) },
        onNavigatePrivacy = { navController.navigate(Screen.PrivacyPolicy.route) },
        onNavigateFaq = { navController.navigate(Screen.Faq.route) },
        unreadMessages = unreadMessages,
    )
}

}

internal fun NavGraphBuilder.infoAndChatGraph(
    navController: NavHostController,
    competitionAvailability: CompetitionAvailability,
) {
composable(Screen.AboutApp.route) {
    AboutAppScreen(
        competitionAvailability = competitionAvailability,
        onBack = { navController.popBackStack() },
    )
}

composable(Screen.PrivacyPolicy.route) {
    PrivacyPolicyScreen(onBack = { navController.popBackStack() })
}

composable(Screen.Faq.route) {
    FaqScreen(
        competitionAvailability = competitionAvailability,
        onBack = { navController.popBackStack() },
    )
}

composable(
    route = Screen.Chat.route,
    arguments = listOf(
        navArgument("conversationId") { type = NavType.StringType },
        navArgument("title") {
            type = NavType.StringType
            nullable = true
            defaultValue = "الإدارة"
        }
    )
) { backStack ->
    val conversationId = backStack.arguments?.getString("conversationId") ?: ""
    val rawTitle = backStack.arguments?.getString("title") ?: "الإدارة"
    val title = URLDecoder.decode(rawTitle, "UTF-8")
    ChatScreen(
        conversationId = conversationId,
        conversationTitle = title,
        onBack = { navController.popBackStack() }
    )
}
}
