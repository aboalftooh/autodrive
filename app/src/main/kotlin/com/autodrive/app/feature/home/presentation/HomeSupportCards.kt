package com.autodrive.app.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import com.autodrive.app.core.designsystem.components.AutoDriveAccent
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButtonTone
import com.autodrive.app.core.designsystem.components.containers.AutoDriveHighlightCard
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.icon.AutoDriveIconSize
import com.autodrive.app.core.designsystem.foundation.radius.AutoDriveRadius
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace

@Suppress("UNUSED_PARAMETER")
@Composable
fun NotificationBell(unreadCount: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(AutoDriveIconSize.TouchTarget),
        contentAlignment = Alignment.Center,
    ) {
        AutoDriveIconButton(
            icon = Icons.Rounded.Notifications,
            contentDescription = "الإشعارات",
            onClick = onClick,
            tone = AutoDriveIconButtonTone.HighEmphasis,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(AutoDriveSpace.Optical)
                .size(AutoDriveSpace.SM)
                .clip(AutoDriveRadius.PillShape)
                .background(AutoDriveBrand.Secondary),
        )
    }
}

@Composable
fun WeeklyCompetitionTeaser(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AutoDriveHighlightCard(
        accent = AutoDriveAccent.Active,
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
        ) {
            Text(
                text = "👑",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.Optical),
            ) {
                Text(
                    text = "المسابقة الأسبوعية",
                    color = AutoDriveBrand.Active,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = description,
                    color = AutoDriveText.Secondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = AutoDriveBrand.Active,
                    modifier = Modifier.size(AutoDriveIconSize.MD),
                )
            }
        }
    }
}

@Composable
fun AiInsightCard(dynamoMessage: String, modifier: Modifier = Modifier) {
    AutoDriveHighlightCard(
        accent = AutoDriveAccent.Insight,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
        ) {
            Box(
                modifier = Modifier
                    .size(AutoDriveIconSize.Hero)
                    .clip(AutoDriveRadius.MediumShape)
                    .background(AutoDriveBrand.Insight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = AutoDriveText.OnBrand,
                    modifier = Modifier.size(AutoDriveIconSize.MD),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.Optical),
            ) {
                Text(
                    text = "نافذة بنزين",
                    color = AutoDriveBrand.Insight,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = dynamoMessage.ifBlank { "جاري تحميل النصائح..." },
                    color = AutoDriveText.Primary,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 4,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
