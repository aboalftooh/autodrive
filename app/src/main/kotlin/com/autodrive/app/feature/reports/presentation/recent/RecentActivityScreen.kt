package com.autodrive.app.feature.reports.presentation.recent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.core.designsystem.components.actions.AutoDriveFab
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveSnackbarContent
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveSearchField
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveBottomNavigation
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveNavigationItem
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveContentWidth
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.patterns.conversation.ConversationItem
import com.autodrive.app.core.designsystem.patterns.header.ScreenHeader
import com.autodrive.app.core.designsystem.patterns.state.EmptyScreen
import com.autodrive.app.core.designsystem.patterns.state.ErrorScreen
import com.autodrive.app.core.designsystem.patterns.state.LoadingScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentActivityScreen(
    onNavigateHome: () -> Unit,
    onNavigateAchievements: () -> Unit,
    onNavigateProfile: () -> Unit,
    onAddClick: () -> Unit = {},
    onOpenConversation: (id: String, title: String) -> Unit = { _, _ -> },
    autoStartNewChat: Boolean = false,
    unreadMessages: Int = 0,
    viewModel: RecentActivityViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error, state.conversations.isNotEmpty()) {
        if (state.error != null && state.conversations.isNotEmpty()) {
            snackbarHostState.showSnackbar(state.error!!)
            viewModel.clearError()
        }
    }

    LaunchedEffect(autoStartNewChat, state.isLoading) {
        if (autoStartNewChat && !state.isLoading) {
            viewModel.openOrCreateConversation { id -> onOpenConversation(id, "الإدارة") }
        }
    }

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> AutoDriveSnackbarContent(data.visuals.message) } },
        topBar = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                ScreenHeader(
                    title = "المحادثات",
                    modifier = Modifier.widthIn(max = AutoDriveContentWidth.Readable).fillMaxWidth().padding(top = AutoDriveSpace.SM),
                    context = {
                        AutoDriveSearchField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQuery,
                            placeholder = "بحث في المحادثات...",
                            searching = state.isRefreshing,
                        )
                    },
                )
            }
        },
        bottomBar = {
            AutoDriveBottomNavigation(
                items = rootNavigationItems(unreadMessages),
                selectedItemId = "messages",
                onItemClick = { item ->
                    when (item.id) {
                        "home" -> onNavigateHome()
                        "achievements" -> onNavigateAchievements()
                        "settings" -> onNavigateProfile()
                    }
                },
                centerAction = { AutoDriveFab(onClick = onAddClick, contentDescription = "محادثة جديدة", icon = Icons.Rounded.Add) },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(modifier = Modifier.widthIn(max = AutoDriveContentWidth.Readable).fillMaxSize()) {
                    when {
                        state.isLoading -> LoadingScreen(label = "جاري تحميل المحادثات")
                        state.error != null && state.conversations.isEmpty() -> ErrorScreen(
                            title = "تعذر تحميل المحادثات",
                            body = state.error.orEmpty(),
                            retryLabel = "إعادة المحاولة",
                            onRetry = {
                                viewModel.clearError()
                                viewModel.refresh()
                            },
                        )
                        state.filteredConversations.isEmpty() -> EmptyScreen(
                            title = if (state.searchQuery.isBlank()) "لا توجد محادثات" else "لا توجد نتائج",
                            body = if (state.searchQuery.isBlank()) "ابدأ محادثة مع الإدارة" else "جرّب كلمة بحث مختلفة",
                            icon = if (state.searchQuery.isBlank()) Icons.Rounded.Message else Icons.Rounded.SearchOff,
                            actionLabel = if (state.searchQuery.isBlank()) "محادثة جديدة" else null,
                            onAction = if (state.searchQuery.isBlank()) ({
                                viewModel.openOrCreateConversation { id -> onOpenConversation(id, "الإدارة") }
                            }) else null,
                        )
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = AutoDriveSpace.LG,
                                vertical = AutoDriveSpace.SM,
                            ),
                        ) {
                            items(state.filteredConversations, key = { it.id }) { conversation ->
                                ConversationItem(
                                    title = conversation.title,
                                    preview = conversation.lastMessage.ifBlank { "ابدأ المحادثة..." },
                                    timestamp = formatConvTime(conversation.lastMessageAt),
                                    unreadCount = conversation.unreadCount,
                                    onClick = { onOpenConversation(conversation.id, conversation.title) },
                                    modifier = Modifier.padding(vertical = AutoDriveSpace.XS),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun rootNavigationItems(unreadMessages: Int) = listOf(
    AutoDriveNavigationItem("home", "الرئيسية", Icons.Rounded.Home),
    AutoDriveNavigationItem("messages", "المحادثات", Icons.Rounded.Message, unreadMessages),
    AutoDriveNavigationItem("achievements", "إنجازاتي", Icons.Rounded.EmojiEvents),
    AutoDriveNavigationItem("settings", "الإعدادات", Icons.Rounded.Settings),
)

private fun formatConvTime(millis: Long): String {
    if (millis == 0L) return ""
    val diff = System.currentTimeMillis() - millis
    val days = diff / 86_400_000
    return when {
        days < 1 -> SimpleDateFormat("HH:mm", Locale("ar")).format(Date(millis))
        days < 7 -> SimpleDateFormat("EEE", Locale("ar")).format(Date(millis))
        else -> SimpleDateFormat("d/M", Locale("ar")).format(Date(millis))
    }
}
