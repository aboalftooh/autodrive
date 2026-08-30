package com.autodrive.app.feature.chat.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.ChatMediaTransferEntity
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.observability.AppLogger
import com.autodrive.app.feature.chat.domain.model.MessageType
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.days

internal data class PreparedChatMedia(
    val mediaUrl: String? = null,
    val mediaMime: String? = null,
    val mediaDurationMs: Long? = null,
    val localPath: String? = null,
    val displayBody: String,
    val transfer: StagedMediaTransfer? = null,
)


internal data class UploadedChatMedia(
    val objectPath: String,
    val compatibilityUrl: String?,
)

internal data class StagedMediaTransfer(
    val localPath: String,
    val mediaMime: String,
    val sizeBytes: Long,
    val contentSha256: String,
    val bucket: String,
    val objectPath: String,
)

internal class MediaTransferException(
    val code: String,
    val retryable: Boolean,
    cause: Throwable? = null,
) : IllegalStateException(code, cause)

@Singleton
class ChatMediaManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: AutoDriveSupabase,
    private val db: AutoDriveDatabase,
) {
    private val mediaDir: File
        get() = File(context.filesDir, "media").also { it.mkdirs() }

    private val downloadClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
    }

    /** Local staging only. No network is allowed on this path. */
    internal suspend fun stageOutgoing(
        type: MessageType,
        content: String,
        messageId: String,
        orgId: String,
    ): PreparedChatMedia = when (type) {
        MessageType.TEXT -> PreparedChatMedia(displayBody = content)
        MessageType.VOICE -> stageVoice(content, messageId, orgId)
        MessageType.IMAGE -> stageImage(content, messageId, orgId)
    }

    internal fun resolveMediaUrl(localPath: String?, originalUrl: String?): String? {
        if (!localPath.isNullOrBlank() && File(localPath).exists()) {
            return File(localPath).toUri().toString()
        }
        return originalUrl
    }

    internal suspend fun cachePendingAdminMedia() = withContext(Dispatchers.IO) {
        val pending = runCatching { db.chatMessageDao().getAdminMediaNeedingDownload() }
            .getOrDefault(emptyList())

        pending.forEach { entity ->
            runCatching {
                val mediaObjectPath = entity.mediaObjectPath
                val url = if (!mediaObjectPath.isNullOrBlank()) {
                    val bucket = if (entity.type == "VOICE") "chat-audio" else "chat-images"
                    supabase.client.storage[bucket].createSignedUrl(mediaObjectPath, 1.days)
                } else {
                    entity.mediaUrl ?: return@runCatching
                }
                val extension = when {
                    entity.mediaMime?.contains("audio") == true -> "m4a"
                    entity.mediaMime?.contains("jpeg") == true -> "jpg"
                    entity.mediaMime?.contains("png") == true -> "png"
                    entity.type == "VOICE" -> "m4a"
                    else -> "jpg"
                }
                val destination = File(mediaDir, "received_${entity.id}.$extension")
                if (!destination.exists()) destination.writeBytes(downloadBytes(url))
                db.chatMessageDao().updateLocalPath(entity.id, destination.absolutePath)
                AppLogger.d(TAG, "cached admin media: ${entity.id}")
            }.onFailure { error ->
                AppLogger.w(TAG, "failed to cache media ${entity.id}: ${error.message}")
            }
        }
    }

    /**
     * Network phase for a previously durable transfer. Retry always reuses objectPath.
     * The object path is the durable reference. A signed URL is minted once only as a temporary
     * compatibility field for older consumers; correctness never depends on its lifetime.
     */
    internal suspend fun uploadTransfer(transfer: ChatMediaTransferEntity): UploadedChatMedia = withContext(Dispatchers.IO) {
        val source = File(transfer.localPath)
        if (!source.isFile) throw MediaTransferException("LOCAL_FILE_MISSING", retryable = false)
        if (source.length() != transfer.sizeBytes) throw MediaTransferException("LOCAL_FILE_SIZE_CHANGED", retryable = false)
        val bytes = source.readBytes()
        val actualHash = sha256(bytes)
        if (actualHash != transfer.contentSha256) throw MediaTransferException("LOCAL_FILE_HASH_CHANGED", retryable = false)

        try {
            supabase.client.storage[transfer.bucket].upload(transfer.objectPath, bytes) { upsert = false }
        } catch (uploadError: Throwable) {
            if (uploadError is CancellationException) throw uploadError
            // Ambiguous upload or already-existing deterministic object: reconcile same identity.
            val reconciled = runCatching {
                val signed = supabase.client.storage[transfer.bucket].createSignedUrl(transfer.objectPath, 7.days)
                sha256(downloadBytes(signed)) == transfer.contentSha256
            }.getOrDefault(false)
            if (!reconciled) {
                throw MediaTransferException("AMBIGUOUS_UPLOAD", retryable = true, cause = uploadError)
            }
        }

        val compatibilityUrl = supabase.client.storage[transfer.bucket].createSignedUrl(transfer.objectPath, 7.days)
        UploadedChatMedia(objectPath = transfer.objectPath, compatibilityUrl = compatibilityUrl)
    }

    private suspend fun stageVoice(content: String, messageId: String, orgId: String): PreparedChatMedia =
        withContext(Dispatchers.IO) {
            val source = File(content)
            if (!source.isFile) throw MediaTransferException("LOCAL_FILE_MISSING", retryable = false)
            requireAllowedSize(source.length())
            val destination = File(mediaDir, "voice_$messageId.m4a")
            source.copyTo(destination, overwrite = true)
            val bytes = destination.readBytes()
            val hash = sha256(bytes)
            val transfer = StagedMediaTransfer(
                localPath = destination.absolutePath,
                mediaMime = "audio/m4a",
                sizeBytes = bytes.size.toLong(),
                contentSha256 = hash,
                bucket = "chat-audio",
                objectPath = stableObjectPath(orgId, messageId, hash, "m4a"),
            )
            PreparedChatMedia(
                mediaMime = transfer.mediaMime,
                mediaDurationMs = audioDurationMs(destination.absolutePath),
                localPath = destination.absolutePath,
                displayBody = "🎤",
                transfer = transfer,
            )
        }

    private suspend fun stageImage(content: String, messageId: String, orgId: String): PreparedChatMedia =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(content)
            val mime = context.contentResolver.getType(uri)?.lowercase() ?: "image/jpeg"
            if (mime !in SUPPORTED_IMAGE_MIME) throw MediaTransferException("UNSUPPORTED_MEDIA_MIME", retryable = false)
            val extension = when (mime) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> throw MediaTransferException("UNSUPPORTED_MEDIA_MIME", retryable = false)
            }
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw MediaTransferException("LOCAL_FILE_MISSING", retryable = false)
            requireAllowedSize(bytes.size.toLong())
            val destination = File(mediaDir, "image_$messageId.$extension")
            destination.writeBytes(bytes)
            val hash = sha256(bytes)
            val transfer = StagedMediaTransfer(
                localPath = destination.absolutePath,
                mediaMime = mime,
                sizeBytes = bytes.size.toLong(),
                contentSha256 = hash,
                bucket = "chat-images",
                objectPath = stableObjectPath(orgId, messageId, hash, extension),
            )
            PreparedChatMedia(
                mediaMime = mime,
                localPath = destination.absolutePath,
                displayBody = "📷",
                transfer = transfer,
            )
        }

    private fun stableObjectPath(orgId: String, messageId: String, contentSha256: String, extension: String): String {
        require(orgId.isNotBlank() && messageId.isNotBlank()) { "MEDIA_SCOPE_ID_MISSING" }
        return "$orgId/$messageId-${contentSha256.take(24)}.$extension"
    }

    private fun requireAllowedSize(size: Long) {
        if (size <= 0L || size > MAX_MEDIA_BYTES) {
            throw MediaTransferException("MEDIA_SIZE_INVALID", retryable = false)
        }
    }

    private suspend fun downloadBytes(url: String): ByteArray {
        require(url.startsWith("https://")) { "HTTPS required for media download" }
        val response = downloadClient.get(url)
        check(response.status.value in 200..299) { "HTTP error ${response.status.value}" }
        return response.readRawBytes().also { bytes ->
            require(bytes.size <= MAX_MEDIA_BYTES) { "file exceeds 15 MB limit" }
        }
    }

    private fun audioDurationMs(path: String): Long = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
        }
    }.getOrNull() ?: 0L

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAG = "ChatMediaManager"
        const val MAX_MEDIA_BYTES = 15 * 1024 * 1024
        val SUPPORTED_IMAGE_MIME = setOf("image/jpeg", "image/png", "image/webp")
    }
}
