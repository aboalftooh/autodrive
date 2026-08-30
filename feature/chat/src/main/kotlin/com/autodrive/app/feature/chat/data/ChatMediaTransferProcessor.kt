package com.autodrive.app.feature.chat.data

import androidx.room.withTransaction
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.ChatMediaTransferEntity
import com.autodrive.app.core.database.entities.ChatMediaTransferStatus
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.data.SyncScope
import com.autodrive.app.core.sync.outbox.ChatSendOutboxPayload
import com.autodrive.app.core.sync.outbox.OutboxOperationType
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class ChatMediaTransferProcessor @Inject constructor(
    private val db: AutoDriveDatabase,
    private val sessionReader: SessionReader,
    private val mediaManager: ChatMediaManager,
) {
    private val json = Json { encodeDefaults = true; explicitNulls = true; ignoreUnknownKeys = true }

    suspend fun processCurrentScope(limit: Int = 10): Int {
        val scope = SyncScope.from(sessionReader.currentSession()) ?: return 0
        val dao = db.chatMediaTransferDao()
        val now = System.currentTimeMillis()
        dao.releaseExpiredClaims(scope.userId, scope.clientId, scope.orgId, now)
        val due = dao.getDue(scope.userId, scope.clientId, scope.orgId, now, limit)
        var completed = 0
        due.forEach { transfer ->
            if (!belongsTo(transfer, scope)) return@forEach
            val claimNow = System.currentTimeMillis()
            val claimed = dao.claim(
                transfer.transferId, scope.userId, scope.clientId, scope.orgId,
                claimNow, claimNow + LEASE_MS,
            )
            if (claimed != 1) return@forEach

            try {
                val uploaded = mediaManager.uploadTransfer(transfer)
                requireCurrentScope(scope)
                db.withTransaction {
                    requireCurrentScope(scope)
                    val message = db.chatMessageDao().getById(transfer.messageId)
                        ?: error("MEDIA_MESSAGE_NOT_FOUND")
                    check(message.senderId == scope.userId) { "MEDIA_MESSAGE_SCOPE_MISMATCH" }
                    val operation = db.pendingOperationDao().findActiveByMutationId(
                        scope.userId, scope.clientId, scope.orgId, transfer.messageId,
                    ) ?: error("MEDIA_SEND_OUTBOX_NOT_FOUND")
                    check(operation.operation == OutboxOperationType.SEND_CHAT_MESSAGE) { "MEDIA_OUTBOX_OPERATION_MISMATCH" }
                    check(operation.status == "PENDING" && operation.attemptCount == 0) { "MEDIA_PAYLOAD_ALREADY_ATTEMPTED" }
                    val payload = json.decodeFromString<ChatSendOutboxPayload>(operation.payload)
                    check(payload.id == transfer.messageId) { "MEDIA_PAYLOAD_IDENTITY_MISMATCH" }
                    val finalized = payload.copy(
                        mediaUrl = uploaded.compatibilityUrl,
                        mediaObjectPath = uploaded.objectPath,
                        mediaMime = transfer.mediaMime,
                        mediaDurationMs = message.mediaDurationMs,
                    )
                    check(db.chatMessageDao().finalizePendingMediaReference(
                        message.id, scope.userId, uploaded.compatibilityUrl, uploaded.objectPath,
                    ) == 1) {
                        "MEDIA_MESSAGE_FINALIZE_FAILED"
                    }
                    check(
                        db.pendingOperationDao().updatePayloadBeforeFirstAttempt(
                            operation.id, scope.userId, scope.clientId, scope.orgId, json.encodeToString(finalized),
                        ) == 1,
                    ) { "MEDIA_OUTBOX_FINALIZE_FAILED" }
                    check(
                        dao.complete(
                            transfer.transferId, scope.userId, scope.clientId, scope.orgId,
                            uploaded.objectPath, System.currentTimeMillis(),
                        ) == 1,
                    ) { "MEDIA_TRANSFER_FINALIZE_FAILED" }
                }
                completed += 1
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (SyncScope.from(sessionReader.currentSession()) != scope) return@forEach
                val retryable = (error as? MediaTransferException)?.retryable ?: true
                val code = (error as? MediaTransferException)?.code ?: error.message.orEmpty().ifBlank { "MEDIA_TRANSFER_FAILED" }
                val attempts = transfer.attemptCount + 1
                if (!retryable || attempts >= MAX_ATTEMPTS) {
                    dao.deadLetter(
                        transfer.transferId, scope.userId, scope.clientId, scope.orgId,
                        attempts, code.take(96), System.currentTimeMillis(),
                    )
                } else {
                    val delay = min(MAX_BACKOFF_MS, BASE_BACKOFF_MS * (1L shl min(attempts - 1, 8)))
                    val retryAt = System.currentTimeMillis() + delay
                    dao.retry(
                        transfer.transferId, scope.userId, scope.clientId, scope.orgId,
                        attempts, retryAt, code.take(96), System.currentTimeMillis(),
                    )
                }
            }
        }
        return completed
    }

    fun cleanupStagedFiles(paths: List<String>) {
        paths.distinct().forEach { path -> runCatching { File(path).delete() } }
    }

    private fun belongsTo(transfer: ChatMediaTransferEntity, scope: SyncScope): Boolean =
        transfer.userId == scope.userId && transfer.clientId == scope.clientId && transfer.orgId == scope.orgId

    private fun requireCurrentScope(scope: SyncScope) {
        check(SyncScope.from(sessionReader.currentSession()) == scope) { "STALE_MEDIA_SCOPE" }
    }

    private companion object {
        const val LEASE_MS = 5L * 60L * 1_000L
        const val BASE_BACKOFF_MS = 5_000L
        const val MAX_BACKOFF_MS = 10L * 60L * 1_000L
        const val MAX_ATTEMPTS = 8
    }
}
