package com.autodrive.app.core.designsystem.components.feedback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.autodrive.app.core.designsystem.components.AutoDriveStatusTone
import com.autodrive.app.core.designsystem.components.color
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

enum class AutoDriveDialogTone { Information, Confirmation, Destructive }
enum class AutoDriveLoadingVariant { Inline, Content }

@Composable
fun AutoDriveBadge(
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    if (count == null) {
        Box(modifier = modifier.size(8.dp)) {
            Surface(modifier = Modifier.matchParentSize(), color = AutoDriveBrand.Primary, shape = CircleShape) {}
        }
    } else {
        Surface(
            color = AutoDriveBrand.Primary,
            contentColor = AutoDriveText.OnBrand,
            shape = AutoDriveRadius.PillShape,
            modifier = modifier.defaultMinSize(minWidth = 18.dp, minHeight = 18.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = AutoDriveSpace.XS)) {
                Text(if (count > 99) "99+" else count.coerceAtLeast(0).toString(), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun AutoDriveStatusChip(
    text: String,
    tone: AutoDriveStatusTone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val color = tone.color()
    Surface(
        color = color.copy(alpha = AutoDriveOpacity.Tint),
        contentColor = color,
        border = BorderStroke(AutoDriveBorder.Thin, if (tone == AutoDriveStatusTone.Neutral) AutoDriveBorderColor.Default else color.copy(alpha = AutoDriveOpacity.Muted)),
        shape = AutoDriveRadius.PillShape,
        modifier = modifier.height(32.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AutoDriveSpace.MD),
            horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.XS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(AutoDriveIconSize.XS))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun AutoDriveSnackbarContent(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = AutoDriveSurface.Overlay,
        shape = AutoDriveRadius.MediumShape,
        border = BorderStroke(AutoDriveBorder.Thin, AutoDriveBorderColor.Default),
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
    ) {
        Row(
            modifier = Modifier.padding(AutoDriveSpace.LG),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
        ) {
            if (icon != null) Icon(icon, null, tint = AutoDriveText.Secondary, modifier = Modifier.size(AutoDriveIconSize.SM))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Primary, modifier = Modifier.weight(1f))
            if (action != null) action()
        }
    }
}

@Composable
fun AutoDriveDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    tone: AutoDriveDialogTone = AutoDriveDialogTone.Information,
    content: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            color = AutoDriveSurface.Raised,
            shape = AutoDriveRadius.ExtraLargeShape,
            border = BorderStroke(
                AutoDriveBorder.Thin,
                if (tone == AutoDriveDialogTone.Destructive) com.autodrive.app.core.designsystem.foundation.color.AutoDriveStatus.Error.copy(alpha = AutoDriveOpacity.High)
                else AutoDriveBorderColor.Default,
            ),
            modifier = modifier.fillMaxWidth().widthIn(max = 360.dp),
        ) {
            Column(
                modifier = Modifier.padding(AutoDriveSpace.X2L),
                verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = AutoDriveText.Primary)
                if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Secondary)
                if (content != null) content()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM, Alignment.End),
                    content = actions,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDriveBottomSheet(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = AutoDriveSurface.Raised,
        contentColor = AutoDriveText.Primary,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = AutoDriveRadius.X2L, topEnd = AutoDriveRadius.X2L),
        dragHandle = {
            Surface(
                color = AutoDriveText.Disabled,
                shape = AutoDriveRadius.PillShape,
                modifier = Modifier.size(width = 32.dp, height = 4.dp),
            ) {}
        },
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxWidth().padding(AutoDriveSpace.XL), verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.LG)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = AutoDriveText.Primary)
            content()
        }
    }
}

@Composable
fun AutoDriveLoadingState(
    modifier: Modifier = Modifier,
    variant: AutoDriveLoadingVariant = AutoDriveLoadingVariant.Content,
    label: String? = null,
) {
    val size = if (variant == AutoDriveLoadingVariant.Content) 32.dp else 20.dp
    val content: @Composable () -> Unit = {
        CircularProgressIndicator(modifier = Modifier.size(size), color = AutoDriveBrand.Primary, strokeWidth = AutoDriveBorder.Strong)
        if (label != null) Text(label, style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Secondary)
    }
    if (variant == AutoDriveLoadingVariant.Content) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD)) { content() }
    } else {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD)) { content() }
    }
}

@Composable
fun AutoDriveEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Inbox,
    action: (@Composable () -> Unit)? = null,
    centered: Boolean = true,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
    ) {
        Icon(icon, null, tint = AutoDriveText.Secondary, modifier = Modifier.size(AutoDriveIconSize.Hero))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = AutoDriveText.Primary, textAlign = if (centered) TextAlign.Center else TextAlign.Start)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Secondary, textAlign = if (centered) TextAlign.Center else TextAlign.Start)
        if (action != null) action()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun FeedbackPreview() = AutoDriveTheme {
    Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD)) {
        AutoDriveStatusChip("مكتمل", AutoDriveStatusTone.Success)
        AutoDriveSnackbarContent("تم حفظ التغييرات")
        AutoDriveEmptyState("لا توجد نتائج", "جرّب تغيير كلمات البحث")
    }
}
