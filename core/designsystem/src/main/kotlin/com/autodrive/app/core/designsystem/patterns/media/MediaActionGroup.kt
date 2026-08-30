package com.autodrive.app.core.designsystem.patterns.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.autodrive.app.core.designsystem.components.AutoDriveStatusTone
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButtonTone
import com.autodrive.app.core.designsystem.components.containers.AutoDriveAlertCard
import com.autodrive.app.core.designsystem.components.containers.AutoDriveCard
import com.autodrive.app.core.designsystem.components.data.AutoDriveStatusIndicator
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

enum class MediaActionState { Idle, MediaSelected, Recording }

@Composable
fun MediaActionGroup(
    state: MediaActionState,
    modifier: Modifier = Modifier,
    mediaLabel: String? = null,
    recordingTime: String? = null,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onRemoveMedia: () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD)) {
        when (state) {
            MediaActionState.Idle -> Row(horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
                AutoDriveIconButton(Icons.Rounded.CameraAlt, "الكاميرا", onCamera)
                AutoDriveIconButton(Icons.Rounded.Image, "المعرض", onGallery)
                AutoDriveIconButton(Icons.Rounded.Mic, "تسجيل صوتي", onStartVoice)
            }
            MediaActionState.MediaSelected -> AutoDriveCard(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
                    Text(mediaLabel ?: "ملف مرفق", style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Primary, modifier = Modifier.weight(1f))
                    AutoDriveIconButton(Icons.Rounded.Delete, "إزالة", onRemoveMedia, tone = AutoDriveIconButtonTone.Destructive)
                }
            }
            MediaActionState.Recording -> AutoDriveAlertCard("جارٍ التسجيل", "", AutoDriveStatusTone.Error) {
                Row(horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
                    AutoDriveStatusIndicator(AutoDriveStatusTone.Error, "تسجيل")
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(recordingTime ?: "00:00", color = AutoDriveText.Primary)
                    }
                    AutoDriveIconButton(Icons.Rounded.Stop, "إيقاف", onStopVoice, tone = AutoDriveIconButtonTone.Destructive)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun MediaActionGroupPreview() = AutoDriveTheme { MediaActionGroup(MediaActionState.Idle, onCamera = {}, onGallery = {}, onStartVoice = {}, onStopVoice = {}, onRemoveMedia = {}) }
