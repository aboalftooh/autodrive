package com.autodrive.app.core.sync.data

import com.autodrive.app.core.database.AutoDriveDatabase
import java.math.BigDecimal
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class LocalDigestRow(
    val entityType: String,
    val entityId: String,
    val partition: String,
    val digest: String,
)

/**
 * v72 anti-entropy projection. Only replicated/server-owned fields participate; local retry,
 * sync, lease, media-local-path and diagnostics fields are deliberately excluded.
 *
 * Digest contract: SHA-256 over ordered fields encoded as `-1:` for null or `<utf8ByteLen>:<value>`.
 */
@Singleton
class CanonicalProjectionDigester @Inject constructor(private val db: AutoDriveDatabase) {
    suspend fun rows(scope: SyncScope): List<LocalDigestRow> {
        val out = mutableListOf<LocalDigestRow>()
        db.autoDriveUserDao().get(scope.userId)?.let { r ->
            out += row("autodrive_users", r.id, listOf(
                r.id, r.userId, r.clientId, r.orgId, r.accountType, r.fullName, r.phone,
                r.bankName, r.bankAccount, r.workshopName, r.specialty, r.workersCount?.toString(),
                r.address, ts(r.createdAt), ts(r.updatedAt),
            ))
        }
        val invoices = db.invoiceDao().getAllByClientIdForSync(scope.clientId)
        invoices.forEach { r -> out += row("invoices", r.id, listOf(
            r.id, r.clientId, dec(r.commission), r.status, r.category, dec(r.totalAmount),
            r.invoiceNumber.toString(), ts(r.createdAt),
        )) }
        db.paymentDao().getAllByClientIdForSync(scope.clientId).forEach { r ->
            out += row("payments", r.id, listOf(r.id, r.clientId, r.invoiceId, dec(r.amount), ts(r.createdAt)))
        }
        db.commissionPaymentDao().getByClientId(scope.clientId).forEach { r ->
            out += row("commission_payments", r.id, listOf(
                r.id, r.clientId, dec(r.amount), r.note, r.invoiceIds, ts(r.createdAt),
            ))
        }
        db.marketerBalanceDao().get(scope.userId)?.let { r ->
            out += row("marketer_balance", r.id, listOf(r.id, r.clientId, dec(r.balance), ts(r.updatedAt)))
        }
        db.balanceTransactionDao().getAllByUserIdForSync(scope.userId)
            .filter { it.syncStatus == "SYNCED" }
            .forEach { r -> out += row("balance_transactions", r.id, listOf(
                r.id, r.clientId, r.type, dec(r.amount), r.description, ts(r.createdAt),
            )) }
        db.withdrawalRequestDao().getAllByUserIdForSync(scope.userId)
            .filter { it.syncStatus == "SYNCED" }
            .forEach { r -> out += row("withdrawal_requests", r.id, listOf(
                r.id, r.clientId, dec(r.amount), r.status, r.bankName, r.bankAccount,
                r.transactionRef, r.note, ts(r.createdAt), ts(r.completedAt),
            )) }
        db.notificationDao().getAllByUserIdForSync(scope.userId)
            .filter { it.readSynced }
            .forEach { r -> out += row("notifications", r.id, listOf(
                r.id, r.userId, r.clientId, r.type, r.title, r.body, r.isRead.toString(), ts(r.createdAt),
            )) }
        val conversations = db.conversationDao().getAllByMarketer(scope.userId)
            .filter { it.clientId == scope.clientId }
        conversations.forEach { r -> out += row("conversations", r.id, listOf(
            r.id, r.clientId, r.subject, r.lastMessage, r.lastMessageAt.toString(),
            r.unreadCount.toString(), r.createdAt.toString(),
        )) }
        if (conversations.isNotEmpty()) {
            db.chatMessageDao().getAllByConversationIdsForSync(conversations.map { it.id })
                .filter { it.status == "SENT" || it.status == "READ" }
                .forEach { r -> out += row("internal_messages", r.id, listOf(
                    r.id, r.conversationId, r.senderId, r.senderType, r.content, r.type,
                    r.isRead.toString(), r.createdAt.toString(), r.mediaUrl, r.mediaMime,
                    r.mediaDurationMs?.toString(), r.mediaObjectPath,
                )) }
        }
        return out.sortedWith(compareBy(LocalDigestRow::entityType, LocalDigestRow::entityId))
    }

    fun partitionDigest(rows: List<LocalDigestRow>): String = sha256(
        rows.sortedBy { it.entityId }.joinToString(separator = "") { "${it.entityId}\t${it.digest}\n" },
    )

    private fun row(type: String, id: String, values: List<String?>): LocalDigestRow =
        LocalDigestRow(type, id, partition(id), digest(values))

    fun partition(id: String): String = sha256(id).take(2)
    fun digest(values: List<String?>): String = sha256(values.joinToString(separator = "") { encode(it) })

    private fun encode(value: String?): String {
        if (value == null) return "-1:"
        val normalized = value
        return "${normalized.toByteArray(Charsets.UTF_8).size}:$normalized"
    }

    private fun dec(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()

    private fun ts(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return runCatching { java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli().toString() }
            .getOrElse { throw IllegalStateException("INVALID_CANONICAL_TIMESTAMP") }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
