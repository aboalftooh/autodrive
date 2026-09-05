package com.autodrive.app.feature.chat.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
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

// ═══════════════════════════════════════════════
// ChatScreen
// ═══════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    conversationTitle: String = "الإدارة",
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    LaunchedEffect(conversationId) {
        viewModel.init(conversationId, conversationTitle)
    }

    val state      by viewModel.uiState.collectAsState()
    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()
    val snackbarHostState  = remember { SnackbarHostState() }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.error) {
        val err = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(err)
        viewModel.clearError()
    }

    // scroll to bottom on new message
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // mark read when screen is visible
    LaunchedEffect(conversationId) {
        viewModel.markRead(conversationId)
    }

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatTopBar(
                title       = state.conversationTitle,
                isTyping    = state.isAdminTyping,
                onBack      = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // ── قائمة الرسائل ──────────────────
            LazyColumn(
                state           = listState,
                modifier        = Modifier.weight(1f).padding(horizontal = 12.dp),
                contentPadding  = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (state.isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AutoDriveFinance.Withdrawable, modifier = Modifier.size(28.dp))
                        }
                    }
                } else if (state.messages.isEmpty()) {
                    item { EmptyChatHint() }
                } else {
                    var lastDate = ""
                    items(state.messages, key = { it.id }) { msg ->
                        val msgDate = formatDateHeader(msg.createdAt)
                        if (msgDate != lastDate) {
                            lastDate = msgDate
                            DateSeparator(msgDate)
                        }
                        MessageBubble(
                            message      = msg,
                            isOwn        = msg.senderType == SenderType.MARKETER,
                            onRetry      = { viewModel.retry(msg.id) },
                            onImageClick = { fullScreenImageUrl = it }
                        )
                    }
                }

                // مؤشر "يكتب..."
                if (state.isAdminTyping) {
                    item { TypingBubble() }
                }
            }

            // ── شريط الكتابة ───────────────────
            ChatComposeBar(
                onSendText  = { text -> viewModel.send(conversationId, MessageType.TEXT, text) },
                onSendVoice = { path -> viewModel.send(conversationId, MessageType.VOICE, path) },
                onSendImage = { uri  -> viewModel.send(conversationId, MessageType.IMAGE, uri) }
            )
        }
    }

    // ─── Full Screen Image Viewer ──────────────
    val context = LocalContext.current
    fullScreenImageUrl?.let { url ->
        FullScreenImageViewer(
            url        = url,
            onDismiss  = { fullScreenImageUrl = null },
            onDownload = { scope.launch { downloadImage(context, url) } }
        )
    }
}


private fun formatDateHeader(millis: Long): String {
    val cal  = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance()
    return when {
        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
        cal.get(Calendar.YEAR)        == today.get(Calendar.YEAR)  -> "اليوم"
        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> "أمس"
        else -> SimpleDateFormat("d MMMM", Locale("ar")).format(Date(millis))
    }
}
