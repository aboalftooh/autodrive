package com.autodrive.app.core.designsystem.foundation.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

object AutoDriveMotion {
    const val Fast = 120
    const val Standard = 200
    const val Expand = 250
    const val Emphasized = 300
    const val Pulse = 500

    fun <T> fast() = tween<T>(durationMillis = Fast, easing = FastOutSlowInEasing)
    fun <T> standard() = tween<T>(durationMillis = Standard, easing = FastOutSlowInEasing)
    fun <T> emphasized() = tween<T>(durationMillis = Emphasized, easing = FastOutSlowInEasing)
}
