package com.autodrive.app.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.autodrive.app.core.designsystem.components.actions.AutoDriveFab
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveBottomNavigation
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveNavigationItem
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveContentWidth
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.patterns.header.ScreenHeader
import com.autodrive.app.feature.competition.domain.model.CompetitionAvailability
import java.util.Calendar

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour >= 4 && hour < 11 -> "صباح الخير"
        hour >= 11 && hour < 16 -> "نهارك سعيد"
        else -> "مساء الخير"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateRecent: () -> Unit,
    onNavigateLog: (String?) -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateNotifications: () -> Unit,
    onNavigateCompetition: () -> Unit,
    competitionAvailability: CompetitionAvailability = CompetitionAvailability.DISABLED,
    onAddClick: () -> Unit,
    unreadMessages: Int = 0,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDynamoMessage()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val firstName = remember(state.userName) {
        state.userName.trim().split(" ").firstOrNull().orEmpty().ifBlank { state.userName }
    }

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        bottomBar = {
            AutoDriveBottomNavigation(
                items = homeRootItems(unreadMessages),
                selectedItemId = "home",
                onItemClick = { item ->
                    when (item.id) {
                        "messages" -> onNavigateRecent()
                        "reports" -> onNavigateLog(null)
                        "settings" -> onNavigateProfile()
                    }
                },
                modifier = Modifier.padding(
                    PaddingValues(
                        start = AutoDriveSpace.LG,
                        end = AutoDriveSpace.LG,
                        bottom = AutoDriveSpace.SM,
                    ),
                ),
                centerAction = {
                    AutoDriveFab(
                        onClick = onAddClick,
                        contentDescription = "محادثة جديدة",
                        icon = Icons.Rounded.Add,
                    )
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AutoDriveSurface.Canvas),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = AutoDriveSpace.MD),
                verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
            ) {
                item {
                    HomeDashboardWidth {
                        ScreenHeader(
                            title = getGreeting(),
                            subtitle = "رحلة موفقة وآمنة اليوم",
                            trailing = {
                                NotificationBell(
                                    unreadCount = state.unreadNotifications,
                                    onClick = onNavigateNotifications,
                                )
                            },
                            titleContent = {
                                Text(
                                    text = buildAnnotatedString {
                                        append(getGreeting())
                                        if (firstName.isNotBlank()) {
                                            append(" ")
                                            withStyle(SpanStyle(color = AutoDriveBrand.Primary)) {
                                                append(firstName)
                                            }
                                        }
                                    },
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = AutoDriveText.Primary,
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }
                item {
                    HomeDashboardWidth(horizontalPadding = true) {
                        PumpHeroCard(
                            state = state,
                            onPump = viewModel::onPumpTapped,
                            onPumpAnimationComplete = viewModel::onPumpAnimationComplete,
                        )
                    }
                }
                if (competitionAvailability != CompetitionAvailability.DISABLED) {
                    item {
                        HomeDashboardWidth(horizontalPadding = true) {
                            WeeklyCompetitionTeaser(
                                description = if (competitionAvailability == CompetitionAvailability.LOCKED) {
                                    "قريباً"
                                } else {
                                    "تحقق من مركزك هذا الأسبوع"
                                },
                                onClick = onNavigateCompetition,
                            )
                        }
                    }
                }
                item {
                    HomeDashboardWidth(horizontalPadding = true) {
                        AiInsightCard(dynamoMessage = state.dynamoMessage)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeDashboardWidth(
    horizontalPadding: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = AutoDriveContentWidth.Dashboard)
                .then(
                    if (horizontalPadding) {
                        Modifier.padding(horizontal = AutoDriveSpace.LG)
                    } else {
                        Modifier
                    },
                ),
        ) {
            content()
        }
    }
}

private fun homeRootItems(unreadMessages: Int) = listOf(
    AutoDriveNavigationItem("home", "الرئيسية", Icons.Rounded.Home),
    AutoDriveNavigationItem("messages", "الرسائل", Icons.Outlined.Message, unreadMessages),
    AutoDriveNavigationItem("reports", "التقارير", Icons.Outlined.BarChart),
    AutoDriveNavigationItem("settings", "الإعدادات", Icons.Outlined.Settings),
)
