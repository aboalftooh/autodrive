package com.autodrive.app.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.autodrive.app.feature.chat.presentation.NewChatDialog
import com.autodrive.app.feature.chat.presentation.NewChatViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.PhoneInput.route,
    navVm: AppNavigationViewModel = hiltViewModel(),
    pendingNavRoute: Flow<String> = emptyFlow()
) {
    val newChatVm: NewChatViewModel = hiltViewModel()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val unreadFlow = remember(backStackEntry?.destination?.route) { navVm.observeUnreadMessages() }
    val unreadMessages by unreadFlow.collectAsState(initial = 0)
    val competitionAvailability by navVm.competitionAvailability.collectAsState()

    LaunchedEffect(Unit) {
        pendingNavRoute.collect { route ->
            var attempts = 0
            while (navController.currentDestination == null && attempts < 60) {
                kotlinx.coroutines.delay(50)
                attempts++
            }
            runCatching { navController.navigate(route) }
        }
    }

    var showNewChatDialog by remember { mutableStateOf(false) }
    val openNewChat: () -> Unit = { showNewChatDialog = true }

    if (showNewChatDialog) {
        NewChatDialog(
            viewModel = newChatVm,
            onDismiss = { showNewChatDialog = false },
            onConversationReady = { conversationId ->
                showNewChatDialog = false
                navController.navigate(Screen.Chat.createRoute(conversationId, "الإدارة"))
            }
        )
    }

    NavHost(navController = navController, startDestination = startDestination) {
        authAndRegistrationGraph(navController, navVm)
        mainGraph(
            navController = navController,
            onOpenNewChat = openNewChat,
            unreadMessages = unreadMessages,
            competitionAvailability = competitionAvailability,
            onRefreshCompetitionAvailability = navVm::refreshCompetitionAvailability,
        )
        infoAndChatGraph(navController, competitionAvailability)
    }
}
