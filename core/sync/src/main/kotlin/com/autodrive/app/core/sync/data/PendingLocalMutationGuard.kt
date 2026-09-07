package com.autodrive.app.core.sync.data

import androidx.room.withTransaction
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.AutoDriveUserEntity
import com.autodrive.app.core.database.entities.BalanceTransactionEntity
import com.autodrive.app.core.database.entities.NotificationEntity
import com.autodrive.app.core.database.entities.WithdrawalRequestEntity
import com.autodrive.app.core.network.dto.AutoDriveUserDto
import com.autodrive.app.core.network.dto.BalanceTransactionDto
import com.autodrive.app.core.network.dto.NotificationDto
import com.autodrive.app.core.network.dto.WithdrawalRequestDto
import com.autodrive.app.core.sync.outbox.OutboxEntityType
import com.autodrive.app.core.sync.outbox.OutboxOperationType
import javax.inject.Inject
import javax.inject.Singleton

class RemoteScopeMismatchException : IllegalStateException("REMOTE_SCOPE_MISMATCH")

@Singleton
class PendingLocalMutationGuard @Inject constructor(
    private val db: AutoDriveDatabase,
) {
    suspend fun profile(scope: SyncScope, remote: AutoDriveUserDto): AutoDriveUserEntity {
        require(remote.userId == scope.userId && remote.clientId == scope.clientId && remote.orgId == scope.orgId)
        val local = db.autoDriveUserDao().getForScope(scope.userId, scope.clientId, scope.orgId)
        val pending = db.pendingOperationDao().findActiveForEntity(
            scope.userId,
            scope.clientId,
            scope.orgId,
            OutboxEntityType.PROFILE,
            scope.userId,
            OutboxOperationType.UPDATE_PROFILE,
        )
        return if (local != null && (local.syncStatus != "SYNCED" || pending != null)) local else remote.toEntity()
    }

    suspend fun notification(scope: SyncScope, remote: NotificationDto): NotificationEntity {
        require(remote.userId == scope.userId && remote.clientId == scope.clientId)
        val mapped = remote.toEntity()
        val local = db.notificationDao().getById(remote.id, scope.userId)
        val pending = db.pendingOperationDao().findActiveForEntity(
            scope.userId,
            scope.clientId,
            scope.orgId,
            OutboxEntityType.NOTIFICATION,
            remote.id,
            OutboxOperationType.MARK_NOTIFICATION_READ,
        )
        return if (local?.isRead == true && (!local.readSynced || pending != null)) {
            mapped.copy(isRead = true, readSynced = false, navRoute = local.navRoute ?: mapped.navRoute)
        } else mapped
    }

    suspend fun balanceTransaction(scope: SyncScope, remote: BalanceTransactionDto): BalanceTransactionEntity {
        require(remote.clientId == scope.clientId && remote.orgId == scope.orgId)
        val local = db.balanceTransactionDao().getById(remote.id)
        return if (local != null && local.syncStatus != "SYNCED") local else remote.toEntity(scope.userId)
    }

    /** Returns null when reconciliation was already applied atomically. */
    suspend fun withdrawal(scope: SyncScope, remote: WithdrawalRequestDto): WithdrawalRequestEntity? {
        require(remote.clientId == scope.clientId && remote.orgId == scope.orgId)
        val key = remote.clientRequestId?.takeIf { it.isNotBlank() }
        val operation = key?.let {
            db.pendingOperationDao().findActiveByMutationId(scope.userId, scope.clientId, scope.orgId, it)
        }
        if (operation?.operation == OutboxOperationType.REQUEST_WITHDRAWAL_RPC) {
            db.withTransaction {
                db.withdrawalRequestDao().deleteByIdForUser(operation.entityId, scope.userId)
                db.withdrawalRequestDao().upsert(remote.toEntity(scope.userId).copy(syncStatus = "SYNCED"))
                db.pendingOperationDao().deleteByMutationId(
                    scope.userId,
                    scope.clientId,
                    scope.orgId,
                    operation.mutationId,
                )
            }
            return null
        }
        val local = db.withdrawalRequestDao().getById(remote.id)
        return if (local != null && local.syncStatus != "SYNCED") local else remote.toEntity(scope.userId)
    }

    fun requireClient(scope: SyncScope, clientId: String) = require(clientId == scope.clientId)
    fun requireClientOrg(scope: SyncScope, clientId: String, orgId: String) =
        require(clientId == scope.clientId && orgId == scope.orgId)

    private fun require(condition: Boolean) {
        if (!condition) throw RemoteScopeMismatchException()
    }
}
