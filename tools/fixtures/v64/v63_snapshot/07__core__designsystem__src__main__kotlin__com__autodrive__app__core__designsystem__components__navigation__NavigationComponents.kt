package com.autodrive.app.core.designsystem.components.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveBadge
import com.autodrive.app.core.designsystem.foundation.border.AutoDriveBorder
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveOpacity
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.icon.AutoDriveIconSize
import com.autodrive.app.core.designsystem.foundation.radius.AutoDriveRadius
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

data class AutoDriveNavigationItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
)

@Composable
fun AutoDriveBottomNavigation(
    items: List<AutoDriveNavigationItem>,
    selectedItemId: String,
    onItemClick: (AutoDriveNavigationItem) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(topStart = AutoDriveRadius.X2L, topEnd = AutoDriveRadius.X2L),
    contentHeight: Dp = 72.dp,
    centerAction: (@Composable () -> Unit)? = null,
    centerActionAfterIndex: Int = 1,
) {
    Surface(
        color = AutoDriveSurface.Base,
        shape = shape,
        border = BorderStroke(AutoDriveBorder.Thin, AutoDriveBorderColor.Default),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.navigationBarsPadding().height(contentHeight).padding(horizontal = AutoDriveSpace.SM),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                BottomNavigationItem(
                    item = item,
                    selected = selectedItemId == item.id,
                    onClick = onItemClick,
                    modifier = Modifier.weight(1f),
                    itemHeight = if (contentHeight < 64.dp) contentHeight else 64.dp,
                    compact = contentHeight < 64.dp,
                )
                if (centerAction != null && index == centerActionAfterIndex) {
                    Box(Modifier.width(AutoDriveSpace.X6L), contentAlignment = Alignment.Center) { centerAction() }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationItem(
    item: AutoDriveNavigationItem,
    selected: Boolean,
    onClick: (AutoDriveNavigationItem) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 64.dp,
    compact: Boolean = false,
) {
    val color = if (selected) AutoDriveBrand.Active else AutoDriveText.Secondary
    Column(
        modifier = modifier
            .height(itemHeight)
            .clip(AutoDriveRadius.MediumShape)
            .clickable { onClick(item) }
            .padding(horizontal = AutoDriveSpace.XS, vertical = if (compact) 4.dp else AutoDriveSpace.SM),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.XS, Alignment.CenterVertically),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(AutoDriveIconSize.XL)
                    .clip(AutoDriveRadius.MediumShape)
                    .background(if (selected) AutoDriveBrand.Active.copy(alpha = AutoDriveOpacity.Tint) else androidx.compose.ui.graphics.Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(item.icon, item.label, tint = color, modifier = Modifier.size(AutoDriveIconSize.MD))
            }
            if (item.badgeCount > 0) AutoDriveBadge(count = item.badgeCount)
        }
        Text(item.label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun AutoDriveTopHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    titleContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(64.dp).padding(horizontal = AutoDriveSpace.LG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
    ) {
        if (leadingContent != null) leadingContent()
        Column(Modifier.weight(1f)) {
            if (titleContent != null) titleContent()
            else Text(title, style = MaterialTheme.typography.headlineMedium, color = AutoDriveText.Primary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AutoDriveText.Secondary)
        }
        if (actions != null) actions()
    }
}

@Composable
fun AutoDriveBackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(56.dp).padding(horizontal = AutoDriveSpace.SM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(AutoDriveIconSize.TouchTarget).clip(AutoDriveRadius.PillShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع", tint = AutoDriveText.Primary, modifier = Modifier.size(AutoDriveIconSize.MD))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = AutoDriveText.Primary, modifier = Modifier.weight(1f))
        if (trailingAction != null) trailingAction()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun BottomNavigationPreview() = AutoDriveTheme {
    AutoDriveBottomNavigation(
        items = listOf(
            AutoDriveNavigationItem("home", "الرئيسية", Icons.Rounded.Home),
            AutoDriveNavigationItem("messages", "الرسائل", Icons.Rounded.Message, 4),
            AutoDriveNavigationItem("reports", "التقارير", Icons.Rounded.BarChart),
        ),
        selectedItemId = "messages",
        onItemClick = {},
    )
}
