package com.autodrive.app.core.designsystem.components.containers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.autodrive.app.core.designsystem.components.AutoDriveAccent
import com.autodrive.app.core.designsystem.components.AutoDriveStatusTone
import com.autodrive.app.core.designsystem.components.color
import com.autodrive.app.core.designsystem.foundation.border.AutoDriveBorder
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveOpacity
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.icon.AutoDriveIconSize
import com.autodrive.app.core.designsystem.foundation.radius.AutoDriveRadius
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

enum class AutoDriveCardState { Default, Selected, Disabled }

@Composable
fun AutoDriveCard(
    modifier: Modifier = Modifier,
    state: AutoDriveCardState = AutoDriveCardState.Default,
    selectedAccent: AutoDriveAccent = AutoDriveAccent.Active,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val enabled = state != AutoDriveCardState.Disabled
    val borderColor = if (state == AutoDriveCardState.Selected) selectedAccent.color().copy(alpha = AutoDriveOpacity.High) else AutoDriveBorderColor.Default
    Surface(
        color = AutoDriveSurface.Base,
        shape = AutoDriveRadius.LargeShape,
        border = BorderStroke(AutoDriveBorder.Thin, borderColor),
        modifier = modifier
            .then(if (state == AutoDriveCardState.Selected) Modifier.semantics { selected = true } else Modifier)
            .then(if (!enabled) Modifier.semantics { disabled() } else Modifier)
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(AutoDriveSpace.LG), content = content)
    }
}

@Composable
fun AutoDriveMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    accent: AutoDriveAccent? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val accentColor = accent?.color() ?: AutoDriveText.Primary
    Surface(
        color = AutoDriveSurface.Raised,
        shape = AutoDriveRadius.LargeShape,
        border = BorderStroke(AutoDriveBorder.Thin, AutoDriveBorderColor.Default),
        modifier = modifier
            .heightIn(min = AutoDriveSpace.X6L + AutoDriveSpace.X3L)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(AutoDriveSpace.LG)) {
            if (icon != null) Icon(icon, null, tint = accentColor, modifier = Modifier.size(AutoDriveIconSize.SM))
            Text(label, style = MaterialTheme.typography.labelMedium, color = AutoDriveText.Secondary)
            Text(value, style = MaterialTheme.typography.displaySmall, color = accentColor)
            if (supportingText != null) Text(supportingText, style = MaterialTheme.typography.bodySmall, color = AutoDriveText.Secondary)
        }
    }
}

@Composable
fun AutoDriveHighlightCard(
    accent: AutoDriveAccent,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = accent.color()
    Surface(
        color = AutoDriveSurface.Raised,
        shape = AutoDriveRadius.ExtraLargeShape,
        border = BorderStroke(AutoDriveBorder.Accent, color.copy(alpha = AutoDriveOpacity.High)),
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) { Column(Modifier.padding(AutoDriveSpace.XL), content = content) }
}

@Composable
fun AutoDriveAlertCard(
    title: String,
    body: String,
    tone: AutoDriveStatusTone,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Info,
    action: (@Composable () -> Unit)? = null,
) {
    val color = tone.color()
    Surface(
        color = AutoDriveSurface.Raised,
        shape = AutoDriveRadius.LargeShape,
        border = BorderStroke(AutoDriveBorder.Accent, color.copy(alpha = AutoDriveOpacity.High)),
        modifier = modifier,
    ) {
        Column(Modifier.padding(AutoDriveSpace.LG)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(AutoDriveIconSize.MD))
            Text(title, style = MaterialTheme.typography.titleMedium, color = AutoDriveText.Primary)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Secondary)
            if (action != null) action()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun ContainerComponentsPreview() = AutoDriveTheme {
    AutoDriveHighlightCard(AutoDriveAccent.Primary, Modifier.fillMaxWidth()) {
        Text("أداء هذا الأسبوع", color = AutoDriveText.Primary)
        Text("1,240,000", style = MaterialTheme.typography.displaySmall, color = AutoDriveText.Primary)
    }
}
