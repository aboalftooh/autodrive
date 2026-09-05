package com.autodrive.app.core.designsystem.components.actions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.autodrive.app.core.designsystem.foundation.border.AutoDriveBorder
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveStatus
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.icon.AutoDriveIconSize
import com.autodrive.app.core.designsystem.foundation.motion.AutoDriveMotion
import com.autodrive.app.core.designsystem.foundation.radius.AutoDriveRadius
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh

private enum class ActionContainer { Primary, Secondary }

enum class AutoDriveTextButtonTone { Neutral, Primary, Destructive }
enum class AutoDriveIconButtonTone { Neutral, HighEmphasis, Active, Destructive }

@Composable
private fun AutoDriveButtonContent(text: String, icon: ImageVector?, loading: Boolean, darkSpinner: Boolean) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(AutoDriveIconSize.SM),
            strokeWidth = AutoDriveBorder.Strong,
            color = if (darkSpinner) AutoDriveText.OnBrand else AutoDriveBrand.Primary,
        )
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(AutoDriveIconSize.SM))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun AutoDrivePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    highlighted: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, AutoDriveMotion.fast(), label = "primary_button_scale")
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = source,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (highlighted) Modifier.shadow(8.dp, AutoDriveRadius.MediumShape) else Modifier),
        shape = AutoDriveRadius.MediumShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = AutoDriveBrand.Primary,
            contentColor = AutoDriveText.OnBrand,
            disabledContainerColor = AutoDriveSurface.Overlay,
            disabledContentColor = AutoDriveText.Disabled,
        ),
        contentPadding = ButtonDefaults.ContentPadding,
    ) { AutoDriveButtonContent(text, icon, loading, darkSpinner = true) }
}

@Composable
fun AutoDriveSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.height(56.dp),
        shape = AutoDriveRadius.MediumShape,
        border = BorderStroke(AutoDriveBorder.Thin, AutoDriveBorderColor.Default),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AutoDriveSurface.Raised,
            contentColor = AutoDriveText.Primary,
            disabledContentColor = AutoDriveText.Disabled,
        ),
    ) { AutoDriveButtonContent(text, icon, loading, darkSpinner = false) }
}

@Composable
fun AutoDriveTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    tone: AutoDriveTextButtonTone = AutoDriveTextButtonTone.Neutral,
    icon: ImageVector? = null,
) {
    val color = when (tone) {
        AutoDriveTextButtonTone.Neutral -> AutoDriveText.Secondary
        AutoDriveTextButtonTone.Primary -> AutoDriveBrand.Primary
        AutoDriveTextButtonTone.Destructive -> AutoDriveStatus.Error
    }
    TextButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.height(AutoDriveIconSize.TouchTarget),
        shape = AutoDriveRadius.MediumShape,
        colors = ButtonDefaults.textButtonColors(contentColor = color, disabledContentColor = AutoDriveText.Disabled),
    ) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(AutoDriveIconSize.SM), strokeWidth = AutoDriveBorder.Strong, color = color)
        else Row(horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(AutoDriveIconSize.SM))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun AutoDriveIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    loading: Boolean = false,
    tone: AutoDriveIconButtonTone = AutoDriveIconButtonTone.Neutral,
) {
    val color = when (tone) {
        AutoDriveIconButtonTone.Neutral -> AutoDriveText.Secondary
        AutoDriveIconButtonTone.HighEmphasis -> AutoDriveText.Primary
        AutoDriveIconButtonTone.Active -> AutoDriveBrand.Active
        AutoDriveIconButtonTone.Destructive -> AutoDriveStatus.Error
    }
    IconButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.size(AutoDriveIconSize.TouchTarget),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = color,
            containerColor = if (selected) color.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent,
            disabledContentColor = AutoDriveText.Disabled,
        ),
    ) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(AutoDriveIconSize.SM), strokeWidth = AutoDriveBorder.Strong, color = color)
        else Icon(icon, contentDescription, modifier = Modifier.size(AutoDriveIconSize.MD))
    }
}

@Composable
fun AutoDriveFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Add,
    contentDescription: String,
    loading: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, AutoDriveMotion.fast(), label = "fab_scale")
    Button(
        onClick = onClick,
        enabled = !loading,
        interactionSource = source,
        modifier = modifier
            .size(56.dp)
            .shadow(12.dp, CircleShape)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = AutoDriveBrand.Primary, contentColor = AutoDriveText.OnBrand),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(AutoDriveIconSize.MD), strokeWidth = AutoDriveBorder.Strong, color = AutoDriveText.OnBrand)
        else Icon(icon, contentDescription, modifier = Modifier.size(AutoDriveIconSize.LG))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun ActionComponentsPreview() = AutoDriveTheme {
    Row(horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
        AutoDrivePrimaryButton("حفظ", {}, Modifier.width(120.dp), icon = Icons.Rounded.Add)
        AutoDriveSecondaryButton("لاحقًا", {}, Modifier.width(120.dp))
        AutoDriveIconButton(Icons.Rounded.Refresh, "تحديث", {})
    }
}
