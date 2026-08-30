package com.autodrive.app.feature.notifications.data

import androidx.room.withTransaction
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.NotificationEntity
import com.autodrive.app.core.database.entities.PendingOperationEntity
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.network.dto.NotificationDto
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.data.SyncScope
import com.autodrive.app.core.sync.outbox.NotificationReadOutboxPayload
import com.autodrive.app.core.sync.outbox.OUTBOX_CONTRACT_VERSION
import com.autodrive.app.core.sync.outbox.OutboxEntityType
import com.autodrive.app.core.sync.outbox.OutboxOperationType
import com.autodrive.app.feature.notifications.domain.model.AppNotification
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.notifications.domain.repository.NotificationRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val db: AutoDriveDatabase,
    private val sessionReader: SessionReader,
) : NotificationRepository {
    private val outboxJson = Json { encodeDefaults = true }

    override fun observeNotifications(userId: String): Flow<List<AppNotification>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return db.notificationDao().observeByUserId(userId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun observeUnreadCount(userId: String): Flow<Int> {
        if (userId.isBlank()) return flowOf(0)
        return db.notificationDao().observeUnreadCount(userId)
    }

    override suspend fun markAsRead(notificationId: String, userId: String) = withContext(Dispatchers.IO) {
        val scope = SyncScope.from(sessionReader.currentSession()) ?: return@withContext
        if (userId != scope.userId) return@withContext
        db.withTransaction {
            check(SyncScope.from(sessionReader.currentSession()) == scope) { "STALE_LOCAL_MUTATION_SCOPE" }
            val notification = db.notificationDao().getById(notificationId, scope.userId)
                ?: return@withTransaction
            check(notification.clientId == scope.clientId) { "NOTIFICATION_SCOPE_MISMATCH" }
            db.notificationDao().markAsRead(notificationId, scope.userId)
            val existing = db.pendingOperationDao().findActiveForEntity(
                scope.userId,
                scope.clientId,
                scope.orgId,
                OutboxEntityType.NOTIFICATION,
                notificationId,
                OutboxOperationType.MARK_NOTIFICATION_READ,
            )
            if (existing == null) {
                val mutationId = java.util.UUID.randomUUID().toString()
                db.pendingOperationDao().insert(
                    PendingOperationEntity(
                        id = "notification_read_${java.util.UUID.randomUUID()}",
                        mutationId = mutationId,
                        userId = scope.userId,
                        clientId = scope.clientId,
                        orgId = scope.orgId,
                        entityType = OutboxEntityType.NOTIFICATION,
                        entityId = notificationId,
                        operation = OutboxOperationType.MARK_NOTIFICATION_READ,
                        payload = outboxJson.encodeToString(NotificationReadOutboxPayload(notificationId)),
                        contractVersion = OUTBOX_CONTRACT_VERSION,
                    )
                )
            }
        }
    }

    override suspend fun markAllRead(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val scope = SyncScope.from(sessionReader.currentSession())
            ?: return@withContext Result.Error("جلسة منتهية. أعد تسجيل الدخول.")
        if (userId != scope.userId) return@withContext Result.Error("جلسة المستخدم غير متطابقة")
        runCatching {
            db.withTransaction {
                check(SyncScope.from(sessionReader.currentSession()) == scope) { "STALE_LOCAL_MUTATION_SCOPE" }
                val targets = db.notificationDao().getUnread(scope.userId)
                targets.forEach { notification ->
                    check(notification.clientId == scope.clientId) { "NOTIFICATION_SCOPE_MISMATCH" }
                    db.notificationDao().markAsRead(notification.id, scope.userId)
                    val existing = db.pendingOperationDao().findActiveForEntity(
                        scope.userId,
                        scope.clientId,
                        scope.orgId,
                        OutboxEntityType.NOTIFICATION,
                        notification.id,
                        OutboxOperationType.MARK_NOTIFICATION_READ,
                    )
                    if (existing == null) {
                        val mutationId = java.util.UUID.randomUUID().toString()
                        db.pendingOperationDao().insert(
                            PendingOperationEntity(
                                id = "notification_read_${java.util.UUID.randomUUID()}",
                                mutationId = mutationId,
                                userId = scope.userId,
                                clientId = scope.clientId,
                                orgId = scope.orgId,
                                entityType = OutboxEntityType.NOTIFICATION,
                                entityId = notification.id,
                                operation = OutboxOperationType.MARK_NOTIFICATION_READ,
                                payload = outboxJson.encodeToString(NotificationReadOutboxPayload(notification.id)),
                                contractVersion = OUTBOX_CONTRACT_VERSION,
                            )
                        )
                    }
                }
            }
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: "خطأ في تحديث الإشعارات", it) }
    }

    // يجلب من Supabase ويُدخل في Room. لا يُعيد الكتابة فوق الإشعارات التي
    // علّمها المستخدم محلياً كمقروءة ولم تُزامَن بعد (read_synced=false)
    override suspend fun syncNotifications(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (userId.isBlank()) return@withContext Result.Error("معرّف المستخدم فارغ")
            runCatching {
                val remote = supabase.client.postgrest["notifications"]
                    .select {
                        filter { eq("user_id", userId) }
                        order("created_at", Order.DESCENDING)
                        limit(50)
                    }
                    .decodeList<NotificationDto>()

                val pendingReadIds = db.notificationDao().getUnsynced(userId).map { it.id }.toSet()

                val entities = remote.map { dto ->
                    NotificationEntity(
                        id        = dto.id,
                        userId    = dto.userId,
                        clientId  = dto.clientId,
                        type      = dto.type,
                        title     = dto.title,
                        body      = dto.body,
                        // إن كان المستخدم علّمها مقروءة محلياً ولم تُزامَن: حافظ على الحالة المحلية
                        isRead    = if (dto.id in pendingReadIds) true else dto.isRead,
                        createdAt = dto.createdAt,
                        readSynced = dto.id !in pendingReadIds,
                        navRoute  = dto.data?.get("nav_route")?.jsonPrimitive?.contentOrNull
                    )
                }
                db.notificationDao().upsertAll(entities)
                Result.Success(Unit)
            }.getOrElse { Result.Error(it.message ?: "خطأ في مزامنة الإشعارات", it) }
        }

    // ─── Mapper ────────────────────────────────────────────────

    private fun NotificationEntity.toDomain() = AppNotification(
        id        = id,
        type      = type,
        title     = title,
        body      = body,
        isRead    = isRead,
        createdAt = createdAt,
        navRoute  = navRoute
    )
}
