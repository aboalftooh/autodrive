package com.autodrive.app.core.designsystem.patterns.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.autodrive.app.core.designsystem.components.containers.AutoDriveCard
import com.autodrive.app.core.designsystem.components.data.AutoDriveAvatar
import com.autodrive.app.core.designsystem.components.data.AutoDriveListRow
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveBadge
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

@Composable
fun ConversationItem(
    title: String,
    preview: String,
    timestamp: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unreadCount: Int = 0,
    identityName: String = title,
) {
    AutoDriveCard(modifier = modifier, onClick = onClick) {
        AutoDriveListRow(
            title = title,
            supportingText = preview,
            leading = { AutoDriveAvatar(identityName) },
            trailing = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(timestamp, style = MaterialTheme.typography.labelSmall, color = AutoDriveText.Secondary)
                    if (unreadCount > 0) AutoDriveBadge(count = unreadCount)
                }
            },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun ConversationItemPreview() = AutoDriveTheme { ConversationItem("ورشة النيل", "تم استلام الطلب", "10:42", {}, unreadCount = 3) }
