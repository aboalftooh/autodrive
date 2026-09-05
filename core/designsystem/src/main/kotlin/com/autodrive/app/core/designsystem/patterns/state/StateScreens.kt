package com.autodrive.app.core.designsystem.patterns.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveEmptyState
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveLoadingState
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

@Composable
fun EmptyScreen(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(AutoDriveSpace.LG), contentAlignment = Alignment.Center) {
        AutoDriveEmptyState(title, body, icon = icon, action = if (actionLabel != null && onAction != null) ({ AutoDrivePrimaryButton(actionLabel, onAction) }) else null)
    }
}

@Composable
fun ErrorScreen(
    title: String,
    body: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().padding(AutoDriveSpace.LG), contentAlignment = Alignment.Center) {
        AutoDriveEmptyState(title, body, icon = Icons.Rounded.ErrorOutline, action = { AutoDrivePrimaryButton(retryLabel, onRetry) })
    }
}

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Box(modifier.fillMaxSize().padding(AutoDriveSpace.LG), contentAlignment = Alignment.Center) { AutoDriveLoadingState(label = label) }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun StateScreensPreview() = AutoDriveTheme { EmptyScreen("لا توجد بيانات", "ستظهر هنا عند توفرها") }
