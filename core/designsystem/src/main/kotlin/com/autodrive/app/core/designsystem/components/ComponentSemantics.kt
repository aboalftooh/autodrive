package com.autodrive.app.core.designsystem.components

import androidx.compose.ui.graphics.Color
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveStatus
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText

/** Presentation-only semantic accents. No domain state belongs here. */
enum class AutoDriveAccent { Primary, Secondary, Active, Info, Insight }
enum class AutoDriveStatusTone { Neutral, Success, Warning, Error, Info }

internal fun AutoDriveAccent.color(): Color = when (this) {
    AutoDriveAccent.Primary -> AutoDriveBrand.Primary
    AutoDriveAccent.Secondary -> AutoDriveBrand.Secondary
    AutoDriveAccent.Active -> AutoDriveBrand.Active
    AutoDriveAccent.Info -> AutoDriveBrand.Info
    AutoDriveAccent.Insight -> AutoDriveBrand.Insight
}

internal fun AutoDriveStatusTone.color(): Color = when (this) {
    AutoDriveStatusTone.Neutral -> AutoDriveText.Secondary
    AutoDriveStatusTone.Success -> AutoDriveStatus.Success
    AutoDriveStatusTone.Warning -> AutoDriveStatus.Warning
    AutoDriveStatusTone.Error -> AutoDriveStatus.Error
    AutoDriveStatusTone.Info -> AutoDriveStatus.Info
}
