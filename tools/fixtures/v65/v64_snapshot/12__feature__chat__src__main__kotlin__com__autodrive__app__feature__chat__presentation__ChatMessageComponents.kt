package com.autodrive.app.feature.chat.presentation

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import coil.imageLoader
import coil.request.ImageRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.autodrive.app.feature.chat.domain.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButtonTone
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButtonTone
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveBackHeader

// ─── Top Bar ──────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatTopBar(title: String, isTyping: Boolean, onBack: () -> Unit) {
    AutoDriveBackHeader(
        title = title,
        onBack = onBack,
        modifier = Modifier.background(AutoDriveSurface.Base),
        titleContent = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AutoDriveText.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isTyping) {
                    Text(
                        text = "يكتب...",
                        style = MaterialTheme.typography.labelSmall,
                        color = AutoDriveFinance.Withdrawable,
                    )
                }
            }
        },
    )
}

// ─── Date Separator ───────────────────────────
@Composable
internal fun DateSeparator(label: String) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = AutoDriveSurface.Raised) {
            Text(
                text     = label,
                style    = MaterialTheme.typography.labelSmall,
                color    = AutoDriveText.Secondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

// ─── Message Bubble ───────────────────────────
@Composable
internal fun MessageBubble(
    message:      ChatMessage,
    isOwn:        Boolean,
    onRetry:      () -> Unit = {},
    onImageClick: (String) -> Unit = {}
) {
    val isFailed = message.status == MessageStatus.FAILED
    // في التخطيط RTL: Start = يمين، End = يسار
    // رسائل المرسل (المسوّق) على اليمين = CenterStart
    // رسائل الإدارة على اليسار = CenterEnd
    Box(
        modifier        = Modifier.fillMaxWidth(),
        contentAlignment = if (isOwn) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Column(
            horizontalAlignment = if (isOwn) Alignment.Start else Alignment.End,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart     = 16.dp,
                    topEnd       = 16.dp,
                    bottomStart  = if (isOwn) 4.dp else 16.dp,
                    bottomEnd    = if (isOwn) 16.dp else 4.dp
                ),
                color = when {
                    isFailed -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                    isOwn    -> AutoDriveFinance.Withdrawable.copy(alpha = 0.18f)
                    else     -> AutoDriveSurface.Raised
                }
            ) {
                when (message.type) {
                    MessageType.TEXT -> Text(
                        text     = message.content,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = AutoDriveText.Primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    MessageType.IMAGE -> {
                        if (message.mediaUrl != null) {
                            AsyncImage(
                                model              = message.mediaUrl,
                                contentDescription = null,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier
                                    .heightIn(max = 240.dp)
                                    .widthIn(max = 240.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImageClick(message.mediaUrl) }
                            )
                        } else {
                            Row(
                                modifier              = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("📷", fontSize = 16.sp)
                                Text("صورة", style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Primary)
                            }
                        }
                    }
                    MessageType.VOICE -> {
                        if (message.mediaUrl != null) {
                            VoiceMessagePlayer(
                                url        = message.mediaUrl,
                                durationMs = message.mediaDurationMs ?: 0L,
                                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        } else {
                            Row(
                                modifier              = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🎤", fontSize = 16.sp)
                                Text("رسالة صوتية", style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Primary)
                            }
                        }
                    }
                }
            }

            // وقت + حالة القراءة
            Row(
                modifier              = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text  = formatTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = AutoDriveText.Disabled
                )
                if (isOwn) {
                    ReadStatusIcon(message.status)
                }
            }

            if (isFailed && isOwn) {
                Row(
                    modifier              = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.error,
                        modifier           = Modifier.size(14.dp)
                    )
                    Text(
                        text  = "فشل الإرسال",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    AutoDriveTextButton(
                        text = "إعادة المحاولة",
                        onClick = onRetry,
                        tone = AutoDriveTextButtonTone.Primary,
                    )
                }
            }
        }
    }
}

// ─── Read Status ✓ ✓✓ ──────────────────────────
@Composable
private fun ReadStatusIcon(status: MessageStatus) {
    val (text, color) = when (status) {
        MessageStatus.PENDING -> "✓"  to AutoDriveText.Disabled
        MessageStatus.SENT    -> "✓✓" to AutoDriveText.Disabled
        MessageStatus.READ    -> "✓✓" to AutoDriveFinance.Withdrawable
        MessageStatus.FAILED  -> "⚠"  to MaterialTheme.colorScheme.error
    }
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
}

// ─── Typing Bubble "..." ─────────────────────
@Composable
internal fun TypingBubble() {
    val inf = rememberInfiniteTransition(label = "typing")
    val dot1 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1")
    val dot2 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(600, 200), RepeatMode.Reverse), label = "d2")
    val dot3 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(600, 400), RepeatMode.Reverse), label = "d3")

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
            color = AutoDriveSurface.Raised
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                listOf(dot1, dot2, dot3).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(AutoDriveText.Secondary.copy(alpha = 0.3f + alpha * 0.7f))
                    )
                }
            }
        }
    }
}

// ─── Empty hint ───────────────────────────────
@Composable
internal fun EmptyChatHint() {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("💬", fontSize = 40.sp)
        Text("ابدأ المحادثة", style = MaterialTheme.typography.titleMedium, color = AutoDriveText.Primary, fontWeight = FontWeight.SemiBold)
        Text("أرسل رسالة للإدارة", style = MaterialTheme.typography.bodySmall, color = AutoDriveText.Secondary, textAlign = TextAlign.Center)
    }
}

// ─── Voice Message Player ─────────────────────
@Composable
private fun VoiceMessagePlayer(url: String, durationMs: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player  = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }
    var isPlaying  by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(url) {
        onDispose { player.release() }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = player.currentPosition
            if (player.playbackState == Player.STATE_ENDED) {
                isPlaying  = false
                positionMs = 0L
                player.seekTo(0)
            }
            delay(200)
        }
    }

    val totalSec = durationMs / 1000
    val curSec   = positionMs / 1000

    Row(
        modifier          = modifier.widthIn(min = 160.dp, max = 220.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AutoDriveIconButton(
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "إيقاف الرسالة الصوتية مؤقتًا" else "تشغيل الرسالة الصوتية",
            onClick = {
                if (isPlaying) { player.pause(); isPlaying = false }
                else           { player.play();  isPlaying = true  }
            },
            modifier = Modifier
                .clip(CircleShape)
                .background(AutoDriveFinance.Withdrawable.copy(alpha = 0.15f)),
            tone = AutoDriveIconButtonTone.Active,
        )
        Column {
            LinearProgressIndicator(
                progress          = { if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f },
                modifier          = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                color             = AutoDriveFinance.Withdrawable,
                trackColor        = AutoDriveFinance.Withdrawable.copy(alpha = 0.2f),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text  = "${curSec / 60}:${"%02d".format(curSec % 60)} / ${totalSec / 60}:${"%02d".format(totalSec % 60)}",
                style = MaterialTheme.typography.labelSmall,
                color = AutoDriveText.Secondary
            )
        }
    }
}

// ═══════════════════════════════════════════════

private fun formatTime(millis: Long): String =
    SimpleDateFormat("hh:mm a", Locale("ar")).format(Date(millis))
