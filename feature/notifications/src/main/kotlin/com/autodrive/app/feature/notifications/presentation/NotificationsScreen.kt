package com.autodrive.app.feature.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.feature.notifications.domain.model.AppNotification
import com.autodrive.app.feature.notifications.presentation.icon
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveLoadingState
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveBackHeader

// ─── Screen ────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // علّم كل الإشعارات كمقروءة فور فتح الشاشة
    LaunchedEffect(Unit) {
        viewModel.markAllAsRead()
    }

    LaunchedEffect(state.error) {
        if (state.error != null) {
            snackbarHostState.showSnackbar(state.error!!)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            AutoDriveBackHeader(
                title = "الإشعارات",
                onBack = onBack,
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    AutoDriveLoadingState()
                }
            }
            state.notifications.isEmpty() -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔔", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(12.dp))
                        Text("لا توجد إشعارات بعد", style = MaterialTheme.typography.bodyLarge, color = AutoDriveText.Secondary)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    contentPadding  = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.notifications, key = { it.id }) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick      = { onNotificationClick(notification) }
                        )
                    }
                }
            }
        }
    }
}

// ─── Row ───────────────────────────────────────
@Composable
private fun NotificationItem(notification: AppNotification, onClick: () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = if (notification.isRead) AutoDriveSurface.Raised else AutoDriveSurface.Overlay,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (notification.isRead) 1.dp else 1.5.dp,
                color = if (notification.isRead) AutoDriveBorderColor.Default else AutoDriveFinance.Withdrawable.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .semantics { stateDescription = if (notification.isRead) "مقروء" else "غير مقروء" }
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Text(
                    text  = notification.icon(),
                    style = MaterialTheme.typography.titleMedium
                )
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AutoDriveFinance.Withdrawable)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text  = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (notification.isRead) AutoDriveText.Secondary else AutoDriveText.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text  = notification.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = AutoDriveText.Secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text  = FormatUtils.formatDate(notification.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = AutoDriveText.Secondary
            )
        }
    }
}
