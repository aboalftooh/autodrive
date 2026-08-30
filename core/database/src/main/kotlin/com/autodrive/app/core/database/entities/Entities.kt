package com.autodrive.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "invoices",
    indices = [Index(value = ["client_id", "category"], name = "index_invoices_client_id_category")],
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "client_id")     val clientId: String,
    val commission: BigDecimal,
    val status: String,
    val category: String,
    @ColumnInfo(name = "total_amount")  val totalAmount: BigDecimal,
    @ColumnInfo(name = "invoice_number") val invoiceNumber: Int,
    @ColumnInfo(name = "created_at")    val createdAt: String
)

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["invoice_id"], name = "index_payments_invoice_id"),
        Index(value = ["client_id"], name = "index_payments_client_id"),
    ],
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "invoice_id") val invoiceId: String,
    val amount: BigDecimal,
    @ColumnInfo(name = "created_at") val createdAt: String
)

@Entity(
    tableName = "commission_payments",
    indices = [Index(value = ["client_id"], name = "index_commission_payments_client_id")],
)
data class CommissionPaymentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "client_id")   val clientId: String,
    val amount: BigDecimal,
    val note: String?,
    @ColumnInfo(name = "invoice_ids") val invoiceIds: String,
    @ColumnInfo(name = "created_at")  val createdAt: String
)

@Entity(tableName = "marketer_balance", indices = [Index("user_id", unique = true)])
data class MarketerBalanceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id")              val userId: String,
    @ColumnInfo(name = "client_id")            val clientId: String,
    val balance: BigDecimal,
    @ColumnInfo(name = "pending_withdrawal")   val pendingWithdrawal: BigDecimal,
    @ColumnInfo(name = "updated_at")           val updatedAt: String
)

@Entity(
    tableName = "balance_transactions",
    indices = [
        Index(value = ["user_id", "created_at"], name = "index_balance_transactions_user_id_created_at"),
        Index(value = ["sync_status"], name = "index_balance_transactions_sync_status"),
    ],
)
data class BalanceTransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id")    val userId: String,
    @ColumnInfo(name = "client_id")  val clientId: String,
    val type: String,
    val amount: BigDecimal,
    val description: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    // SYNCED = confirmed on server | PENDING = waiting to be sent
    @ColumnInfo(name = "sync_status") val syncStatus: String = "SYNCED"
)

@Entity(
    tableName = "withdrawal_requests",
    indices = [
        Index(value = ["user_id", "created_at"], name = "index_withdrawal_requests_user_id_created_at"),
        Index(value = ["sync_status"], name = "index_withdrawal_requests_sync_status"),
        Index(value = ["user_id", "status"], name = "index_withdrawal_requests_user_id_status"),
    ],
)
data class WithdrawalRequestEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id")          val userId: String,
    @ColumnInfo(name = "client_id")        val clientId: String,
    val amount: BigDecimal,
    val status: String,
    @ColumnInfo(name = "bank_name")        val bankName: String,
    @ColumnInfo(name = "bank_account")     val bankAccount: String,
    @ColumnInfo(name = "transaction_ref")  val transactionRef: String? = null,
    val note: String?,
    @ColumnInfo(name = "created_at")       val createdAt: String,
    @ColumnInfo(name = "completed_at")     val completedAt: String?,
    @ColumnInfo(name = "sync_status")      val syncStatus: String = "SYNCED"
)

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["user_id", "created_at"], name = "index_notifications_user_id_created_at"),
        Index(value = ["user_id", "is_read", "read_synced"], name = "index_notifications_user_id_is_read_read_synced"),
    ],
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id")    val userId: String,
    @ColumnInfo(name = "client_id")  val clientId: String,
    val type: String,
    val title: String,
    val body: String,
    @ColumnInfo(name = "is_read")    val isRead: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String,
    // false = تم تعليمه مقروءاً محلياً لكن لم يُزامَن بعد
    @ColumnInfo(name = "read_synced") val readSynced: Boolean = true,
    // بند 9: شاشة الربط من data.nav_route — للتوجيه عند الضغط من قائمة الإشعارات الداخلية
    @ColumnInfo(name = "nav_route")  val navRoute: String? = null
)

@Entity(
    tableName = "autodrive_users",
    indices = [Index(value = ["user_id"], name = "index_autodrive_users_user_id")],
)
data class AutoDriveUserEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id")        val userId: String,
    @ColumnInfo(name = "client_id")      val clientId: String,
    @ColumnInfo(name = "org_id")         val orgId: String,
    @ColumnInfo(name = "account_type")   val accountType: String,
    @ColumnInfo(name = "full_name")      val fullName: String,
    val phone: String,
    @ColumnInfo(name = "bank_name")      val bankName: String?,
    @ColumnInfo(name = "bank_account")   val bankAccount: String?,
    @ColumnInfo(name = "workshop_name")  val workshopName: String?,
    val specialty: String?,
    @ColumnInfo(name = "workers_count")  val workersCount: Int?,
    val address: String?,
    @ColumnInfo(name = "created_at")     val createdAt: String,
    @ColumnInfo(name = "updated_at")     val updatedAt: String,
    @ColumnInfo(name = "sync_status")    val syncStatus: String = "SYNCED"
)

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["marketer_id", "last_message_at"], name = "index_conversations_marketer_id_last_message_at"),
        Index(value = ["marketer_id", "created_at"], name = "index_conversations_marketer_id_created_at"),
        Index(value = ["client_id"], name = "index_conversations_client_id"),
    ],
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "marketer_id")      val marketerId: String,
    @ColumnInfo(name = "client_id")        val clientId: String,
    val title: String                    = "الإدارة",
    val subject: String                  = "",
    @ColumnInfo(name = "last_message")     val lastMessage: String = "",
    @ColumnInfo(name = "last_message_at")  val lastMessageAt: Long = 0L,
    @ColumnInfo(name = "unread_count")     val unreadCount: Int    = 0,
    @ColumnInfo(name = "created_at")       val createdAt: Long
)

@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["conversation_id", "created_at"], name = "index_chat_messages_conversation_id_created_at"),
        Index(value = ["status", "created_at"], name = "index_chat_messages_status_created_at"),
        Index(value = ["sender_type", "status", "type"], name = "index_chat_messages_sender_type_status_type"),
    ],
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "conversation_id")  val conversationId: String,
    @ColumnInfo(name = "sender_id")        val senderId: String,
    @ColumnInfo(name = "sender_type")      val senderType: String,   // MARKETER | ADMIN
    val content: String,
    val type: String                     = "TEXT",                   // TEXT | VOICE | IMAGE
    @ColumnInfo(name = "is_read")          val isRead: Boolean       = false,
    @ColumnInfo(name = "created_at")       val createdAt: Long,
    val status: String                   = "PENDING",                // PENDING | SENT | READ
    @ColumnInfo(name = "media_url")        val mediaUrl: String?     = null,
    @ColumnInfo(name = "media_mime")       val mediaMime: String?    = null,
    @ColumnInfo(name = "media_duration_ms") val mediaDurationMs: Long? = null,
    @ColumnInfo(name = "media_object_path") val mediaObjectPath: String? = null,
    // مسار الملف المحلي — يُستخدم بدلاً من media_url إذا توفّر (Offline-first)
    @ColumnInfo(name = "local_path")       val localPath: String?    = null,
)

// ─── نصيحة اليوم — كاش محلي لرسائل Supabase ────────
@Entity(
    tableName = "dynamo_content",
    indices = [Index(value = ["is_active"], name = "index_dynamo_content_is_active")],
)
data class DynamoContentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "content_type")  val contentType: String,
    @ColumnInfo(name = "audience_type") val audienceType: String,
    val specialty: String = "general",
    val message: String,
    val priority: Int = 1,
    @ColumnInfo(name = "is_active")     val isActive: Boolean = true,
    @ColumnInfo(name = "created_at")    val createdAt: String = "",
    @ColumnInfo(name = "synced_at")     val syncedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "weekly_leaderboard_cache",
    indices = [Index(value = ["rank"], name = "index_weekly_leaderboard_cache_rank")],
)
data class WeeklyLeaderboardEntity(
    @PrimaryKey val id: String,
    val rank: Int,
    @ColumnInfo(name = "total_amount") val totalAmount: BigDecimal,
    @ColumnInfo(name = "is_me") val isMe: Boolean,
    @ColumnInfo(name = "week_number") val weekNumber: Int,
    @ColumnInfo(name = "synced_at") val syncedAt: Long = System.currentTimeMillis()
)

// طابور العمليات المعلقة — كل صف مملوك صراحةً لنطاق مزامنة واحد
@Entity(
    tableName = "pending_operations",
    indices = [
        Index(
            value = ["user_id", "client_id", "org_id", "status", "next_retry_at", "created_at"],
            name = "index_pending_operations_scope_status_retry_created",
        ),
        Index(
            value = ["user_id", "client_id", "org_id", "entity_type", "entity_id", "status"],
            name = "index_pending_operations_scope_entity_status",
        ),
        Index(
            value = ["user_id", "client_id", "org_id", "mutation_id"],
            unique = true,
            name = "index_pending_operations_scope_mutation",
        ),
    ],
)
data class PendingOperationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "mutation_id") val mutationId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    val operation: String,
    val payload: String,
    @ColumnInfo(name = "contract_version") val contractVersion: Int = 1,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING",
    @ColumnInfo(name = "attempt_count") val attemptCount: Int = 0,
    @ColumnInfo(name = "next_retry_at") val nextRetryAt: Long = 0L,
    @ColumnInfo(name = "lease_until") val leaseUntil: Long = 0L,
    @ColumnInfo(name = "depends_on_mutation_id") val dependsOnMutationId: String? = null,
    @ColumnInfo(name = "last_error_code") val lastErrorCode: String? = null,
    @ColumnInfo(name = "last_error_message") val lastErrorMessage: String? = null,
)
