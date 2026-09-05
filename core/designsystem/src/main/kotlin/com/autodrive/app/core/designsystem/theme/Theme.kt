package com.autodrive.app.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveStatus
import com.autodrive.app.core.designsystem.foundation.typography.AutoDriveTypography

private val AutoDriveColorScheme = darkColorScheme(
    primary = AutoDriveBrand.Primary,
    onPrimary = AutoDriveText.OnBrand,
    secondary = AutoDriveBrand.Secondary,
    onSecondary = AutoDriveText.OnBrand,
    tertiary = AutoDriveBrand.Info,
    background = AutoDriveSurface.Canvas,
    surface = AutoDriveSurface.Base,
    surfaceVariant = AutoDriveSurface.Raised,
    onBackground = AutoDriveText.Primary,
    onSurface = AutoDriveText.Primary,
    onSurfaceVariant = AutoDriveText.Secondary,
    outline = AutoDriveBorderColor.Default,
    error = AutoDriveStatus.Error,
)

@Composable
fun AutoDriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AutoDriveColorScheme,
        typography = AutoDriveTypography,
        content = content,
    )
}
