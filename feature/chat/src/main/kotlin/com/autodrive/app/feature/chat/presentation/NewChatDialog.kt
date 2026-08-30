package com.autodrive.app.feature.chat.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveSecondaryButton
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveDialog
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveTextField
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveStatus
import com.autodrive.app.core.designsystem.patterns.media.MediaActionGroup
import com.autodrive.app.core.designsystem.patterns.media.MediaActionState
import com.autodrive.app.feature.chat.domain.model.MessageType
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

private enum class DialogRecordState { IDLE, RECORDING, HAS_VOICE }

@Composable
fun NewChatDialog(
    onDismiss: () -> Unit,
    onConversationReady: (conversationId: String) -> Unit,
    viewModel: NewChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var text by remember { mutableStateOf("") }
    var recordState by remember { mutableStateOf(DialogRecordState.IDLE) }
    var voicePath by remember { mutableStateOf("") }
    var recordingMs by remember { mutableLongStateOf(0L) }
    var pendingMedia by remember { mutableStateOf<Pair<MessageType, String>?>(null) }
    val recorderRef = remember { mutableStateOf<MediaRecorder?>(null) }
    val recFileRef = remember { mutableStateOf<File?>(null) }

    LaunchedEffect(recordState) {
        if (recordState == DialogRecordState.RECORDING) {
            recordingMs = 0L
            while (recordState == DialogRecordState.RECORDING) {
                delay(1000)
                recordingMs += 1000
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { recorderRef.value?.runCatching { stop(); release() } }
    }

    val audioPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecorder(context, recorderRef, recFileRef) { recordState = DialogRecordState.RECORDING }
    }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) cameraUri?.toString()?.let { pendingMedia = MessageType.IMAGE to it }
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = createCaptureFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pendingMedia = MessageType.IMAGE to it.toString() }
    }

    fun stopRecording() {
        recorderRef.value?.runCatching { stop(); release() }
        recorderRef.value = null
        voicePath = recFileRef.value?.absolutePath ?: ""
        recordState = if (voicePath.isNotBlank()) DialogRecordState.HAS_VOICE else DialogRecordState.IDLE
        if (voicePath.isNotBlank()) pendingMedia = MessageType.VOICE to voicePath
    }
    fun openCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val file = createCaptureFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else cameraPermLauncher.launch(Manifest.permission.CAMERA)
    }
    fun startVoice() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) startRecorder(context, recorderRef, recFileRef) { recordState = DialogRecordState.RECORDING }
        else audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    fun removeMedia() {
        pendingMedia = null
        voicePath = ""
        recordState = DialogRecordState.IDLE
    }
    fun send() {
        val media = pendingMedia
        viewModel.createAndSend(text, media?.first, media?.second, onConversationReady)
    }

    val mediaState = when {
        recordState == DialogRecordState.RECORDING -> MediaActionState.Recording
        pendingMedia != null -> MediaActionState.MediaSelected
        else -> MediaActionState.Idle
    }
    val recordingLabel = String.format(
        Locale.US,
        "%02d:%02d",
        (recordingMs / 1000) / 60,
        (recordingMs / 1000) % 60,
    )

    AutoDriveDialog(
        title = "محادثة جديدة",
        onDismissRequest = onDismiss,
        content = {
            AutoDriveTextField(
                value = text,
                onValueChange = { text = it },
                label = "الرسالة",
                placeholder = "اكتب رسالتك...",
                singleLine = false,
                maxLines = 5,
            )
            MediaActionGroup(
                state = mediaState,
                mediaLabel = pendingMedia?.first?.let { if (it == MessageType.VOICE) "رسالة صوتية جاهزة" else "صورة جاهزة" },
                recordingTime = recordingLabel,
                onCamera = ::openCamera,
                onGallery = { galleryLauncher.launch("image/*") },
                onStartVoice = ::startVoice,
                onStopVoice = ::stopRecording,
                onRemoveMedia = ::removeMedia,
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = AutoDriveStatus.Error)
            }
        },
        actions = {
            AutoDriveSecondaryButton("إلغاء", onDismiss, enabled = !state.isCreating)
            AutoDrivePrimaryButton(
                text = "إرسال",
                onClick = ::send,
                enabled = text.isNotBlank() || pendingMedia != null,
                loading = state.isCreating,
                icon = Icons.AutoMirrored.Rounded.Send,
            )
        },
    )
}

private fun startRecorder(
    context: Context,
    recorderRef: androidx.compose.runtime.MutableState<MediaRecorder?>,
    recFileRef: androidx.compose.runtime.MutableState<File?>,
    onStarted: () -> Unit
) {
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
        recorderRef.value = rec
        recFileRef.value  = file
        onStarted()
    }
}

private fun createCaptureFile(context: Context): File {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    return File(dir, "img_${System.currentTimeMillis()}.jpg")
}
