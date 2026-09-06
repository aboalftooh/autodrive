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
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButtonTone
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButtonTone
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButtonTone
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveDialog
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveTextField
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveTextFieldLayout

// ChatComposeBar — شريط الكتابة
// ═══════════════════════════════════════════════
private enum class ComposeState { IDLE, RECORDING, HAS_VOICE }

@Composable
fun ChatComposeBar(
    onSendText:  (String) -> Unit,
    onSendVoice: (String) -> Unit,
    onSendImage: (String) -> Unit
) {
    val context = LocalContext.current

    var text         by remember { mutableStateOf("") }
    var composeState by remember { mutableStateOf(ComposeState.IDLE) }
    var voicePath    by remember { mutableStateOf("") }
    var recordingMs  by remember { mutableLongStateOf(0L) }

    val recorderRef = remember { mutableStateOf<MediaRecorder?>(null) }
    val recFileRef  = remember { mutableStateOf<File?>(null) }

    LaunchedEffect(composeState) {
        if (composeState == ComposeState.RECORDING) {
            recordingMs = 0L
            while (composeState == ComposeState.RECORDING) {
                delay(1000); recordingMs += 1000
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorderRef.value?.runCatching { stop(); release() }
        }
    }

    // أذن المايكروفون
    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val pair = createAndStartRecorder(context)
            if (pair != null) {
                recorderRef.value = pair.first
                recFileRef.value  = pair.second
                composeState      = ComposeState.RECORDING
            }
        }
    }

    // الكاميرا
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        if (saved) cameraUri?.toString()?.let { onSendImage(it) }
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = createCaptureFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onSendImage(it.toString()) } }

    // إذن المعرض للأجهزة القديمة (API < 33)
    val mediaReadPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) galleryLauncher.launch("image/*") }

    fun launchGallery() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val perm = Manifest.permission.READ_EXTERNAL_STORAGE
            val hasPerm = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) galleryLauncher.launch("image/*")
            else mediaReadPermLauncher.launch(perm)
        } else {
            galleryLauncher.launch("image/*")
        }
    }

    var showImageMenu by remember { mutableStateOf(false) }

    fun stopRecording() {
        recorderRef.value?.runCatching { stop(); release() }
        recorderRef.value = null
        voicePath    = recFileRef.value?.absolutePath ?: ""
        composeState = if (voicePath.isNotBlank()) ComposeState.HAS_VOICE else ComposeState.IDLE
    }

    fun startMic() {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val pair = createAndStartRecorder(context)
            if (pair != null) { recorderRef.value = pair.first; recFileRef.value = pair.second; composeState = ComposeState.RECORDING }
        } else audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Surface(
        color    = AutoDriveSurface.Base,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AutoDriveBorderColor.Default, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            when (composeState) {
                ComposeState.RECORDING -> RecordingBar(
                    elapsedMs = recordingMs,
                    onStop    = { stopRecording() }
                )
                ComposeState.HAS_VOICE -> VoiceReadyBar(
                    durationMs = recordingMs,
                    onCancel   = { voicePath = ""; composeState = ComposeState.IDLE },
                    onSend     = { onSendVoice(voicePath); voicePath = ""; composeState = ComposeState.IDLE }
                )
                ComposeState.IDLE -> Row(
                    modifier          = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // زر الكاميرا (يسار في RTL)
                    AutoDriveIconButton(
                        icon = Icons.Rounded.CameraAlt,
                        contentDescription = "صورة",
                        onClick = { showImageMenu = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AutoDriveFinance.Pending.copy(alpha = 0.12f)),
                        tone = AutoDriveIconButtonTone.Active,
                    )

                    // حقل النص
                    AutoDriveTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = null,
                        placeholder = "اكتب رسالة...",
                        modifier = Modifier.weight(1f),
                        singleLine = false,
                        maxLines = 5,
                        layout = AutoDriveTextFieldLayout.CompactMultiline,
                    )

                    // زر الإرسال أو المايكروفون (يمين في RTL)
                    if (text.isNotBlank()) {
                        AutoDriveIconButton(
                            icon = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "إرسال",
                            onClick = { onSendText(text.trim()); text = "" },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AutoDriveFinance.Withdrawable),
                            tone = AutoDriveIconButtonTone.HighEmphasis,
                        )
                    } else {
                        AutoDriveIconButton(
                            icon = Icons.Rounded.Mic,
                            contentDescription = "تسجيل",
                            onClick = { startMic() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AutoDriveFinance.Withdrawable.copy(alpha = 0.12f)),
                            tone = AutoDriveIconButtonTone.Active,
                        )
                    }
                }
            }
        }
    }

    // قائمة خيارات الصورة
    if (showImageMenu) {
        AutoDriveDialog(
            title = "إرفاق صورة",
            onDismissRequest = { showImageMenu = false },
            content = {
                Column {
                    AutoDriveTextButton(
                        text = "التقاط صورة",
                        onClick = {
                            showImageMenu = false
                            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                val f = createCaptureFile(context)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", f)
                                cameraUri = uri
                                cameraLauncher.launch(uri)
                            } else cameraPermLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        tone = AutoDriveTextButtonTone.Primary,
                        icon = Icons.Rounded.CameraAlt,
                    )
                    AutoDriveTextButton(
                        text = "اختيار من المعرض",
                        onClick = { showImageMenu = false; launchGallery() },
                        modifier = Modifier.fillMaxWidth(),
                        tone = AutoDriveTextButtonTone.Primary,
                        icon = Icons.Rounded.Photo,
                    )
                }
            },
            actions = {
                AutoDriveTextButton(text = "إلغاء", onClick = { showImageMenu = false })
            },
        )
    }
}

// ─── Recording Bar ────────────────────────────
@Composable
private fun RecordingBar(elapsedMs: Long, onStop: () -> Unit) {
    val inf   = rememberInfiniteTransition(label = "rec")
    val scale by inf.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "s")
    val mins  = (elapsedMs / 1000) / 60
    val secs  = (elapsedMs / 1000) % 60
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🎤", fontSize = 20.sp, modifier = Modifier.scale(scale))
            Text(
                text       = String.format(Locale.US, "%02d:%02d", mins, secs),
                style      = MaterialTheme.typography.titleMedium,
                color      = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
        AutoDrivePrimaryButton(
            text = "إيقاف",
            onClick = onStop,
            icon = Icons.Rounded.Stop,
            tone = AutoDrivePrimaryButtonTone.Destructive,
        )
    }
}

// ─── Voice Ready Bar ──────────────────────────
@Composable
private fun VoiceReadyBar(durationMs: Long, onCancel: () -> Unit, onSend: () -> Unit) {
    val mins = (durationMs / 1000) / 60
    val secs = (durationMs / 1000) % 60
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AutoDriveIconButton(
            icon = Icons.Rounded.Delete,
            contentDescription = "إلغاء التسجيل الصوتي",
            onClick = onCancel,
            tone = AutoDriveIconButtonTone.Destructive,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🎤", fontSize = 18.sp)
            Text(String.format(Locale.US, "%02d:%02d", mins, secs), style = MaterialTheme.typography.titleMedium, color = AutoDriveFinance.Withdrawable, fontWeight = FontWeight.Bold)
        }
        AutoDriveIconButton(
            icon = Icons.AutoMirrored.Rounded.Send,
            contentDescription = "إرسال التسجيل الصوتي",
            onClick = onSend,
            modifier = Modifier.clip(CircleShape).background(AutoDriveFinance.Withdrawable),
            tone = AutoDriveIconButtonTone.HighEmphasis,
        )
    }
}

// ═══════════════════════════════════════════════

private fun createAndStartRecorder(context: Context): Pair<MediaRecorder, File>? =
    runCatching {
        val dir  = File(context.cacheDir, "recordings").apply { mkdirs() }
        val file = File(dir, "rec_${System.currentTimeMillis()}.m4a")
        val rec  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
                   else @Suppress("DEPRECATION") MediaRecorder()
        rec.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare(); start()
        }
        Pair(rec, file)
    }.getOrNull()

private fun createCaptureFile(context: Context): File {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    return File(dir, "img_${System.currentTimeMillis()}.jpg")
}
