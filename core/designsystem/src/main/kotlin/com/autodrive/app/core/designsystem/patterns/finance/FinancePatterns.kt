package com.autodrive.app.core.designsystem.patterns.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.autodrive.app.core.designsystem.components.AutoDriveStatusTone
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveSecondaryButton
import com.autodrive.app.core.designsystem.components.containers.AutoDriveAlertCard
import com.autodrive.app.core.designsystem.components.data.AutoDriveListRow
import com.autodrive.app.core.designsystem.components.data.AutoDriveStatSize
import com.autodrive.app.core.designsystem.components.data.AutoDriveStatValue
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveStatusChip
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

@Composable
fun TransactionRow(
    title: String,
    amount: String,
    metadata: String,
    tone: AutoDriveStatusTone,
    modifier: Modifier = Modifier,
    statusLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    AutoDriveListRow(
        title = title,
        supportingText = metadata,
        onClick = onClick,
        modifier = modifier,
        trailing = {
            Column {
                AutoDriveStatValue(amount, size = AutoDriveStatSize.Small)
                if (statusLabel != null) AutoDriveStatusChip(statusLabel, tone)
            }
        },
    )
}

@Composable
fun PendingRequestCard(
    title: String,
    amount: String,
    metadata: String,
    statusLabel: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    AutoDriveAlertCard(title = title, body = metadata, tone = AutoDriveStatusTone.Warning, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD)) {
            AutoDriveStatusChip(statusLabel, AutoDriveStatusTone.Warning)
            AutoDriveStatValue(amount, size = AutoDriveStatSize.Medium)
            if (primaryActionLabel != null || secondaryActionLabel != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
                    if (secondaryActionLabel != null && onSecondaryAction != null) AutoDriveSecondaryButton(secondaryActionLabel, onSecondaryAction, Modifier.weight(1f), enabled = !loading)
                    if (primaryActionLabel != null && onPrimaryAction != null) AutoDrivePrimaryButton(primaryActionLabel, onPrimaryAction, Modifier.weight(1f), loading = loading)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun FinancePatternsPreview() = AutoDriveTheme {
    PendingRequestCard("طلب سحب", "250,000", "منذ ساعتين", "قيد المراجعة", primaryActionLabel = "عرض", onPrimaryAction = {})
}
