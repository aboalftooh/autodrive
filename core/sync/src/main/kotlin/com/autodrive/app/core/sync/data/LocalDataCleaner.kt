package com.autodrive.app.core.sync.data

import androidx.room.withTransaction
import com.autodrive.app.core.database.AutoDriveDatabase
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDataCleaner @Inject constructor(
    private val db: AutoDriveDatabase,
) {
    /** Removes only [scope]'s durable state. All Room deletions commit or roll back together. */
    suspend fun clearAccount(scope: SyncScope) {
        val stagedMedia = db.chatMediaTransferDao().getLocalPathsForScope(
            scope.userId, scope.clientId, scope.orgId,
        )
        db.withTransaction {
            db.paymentDao().deleteByClientId(scope.clientId)
            db.invoiceDao().deleteByClientId(scope.clientId)
            db.commissionPaymentDao().deleteByClientId(scope.clientId)

            db.marketerBalanceDao().deleteByUserId(scope.userId)
            db.balanceTransactionDao().deleteByUserId(scope.userId)
            db.withdrawalRequestDao().deleteByUserId(scope.userId)
            db.notificationDao().deleteByUserId(scope.userId)

            val conversationIds = db.conversationDao().getAllByMarketer(scope.userId).map { it.id }
            if (conversationIds.isNotEmpty()) {
                db.chatMessageDao().deleteByConversationIds(conversationIds)
            }
            db.conversationDao().deleteByMarketer(scope.userId)

            // Queue/cursor ownership is exact-scope; never delete another account's rows.
            db.pendingOperationDao().deleteForScope(scope.userId, scope.clientId, scope.orgId)
            db.syncCursorDao().deleteForScope(scope.userId, scope.clientId, scope.orgId)
            db.syncInboxDao().deleteForScope(scope.userId, scope.clientId, scope.orgId)
            db.chatRecoveryCheckpointDao().deleteForScope(scope.userId, scope.clientId, scope.orgId)
            db.chatMediaTransferDao().deleteForScope(scope.userId, scope.clientId, scope.orgId)
            db.syncBootstrapDao().deleteStagingForScope(scope.userId, scope.clientId, scope.orgId)
            db.syncBootstrapDao().deleteStateForScope(scope.userId, scope.clientId, scope.orgId)
            db.syncReconciliationStateDao().deleteForScope(scope.userId, scope.clientId, scope.orgId)
            db.syncObservabilityDao().deleteForScope(scope.userId, scope.clientId, scope.orgId)
            db.autoDriveUserDao().deleteByUserId(scope.userId)

            // Shared non-principal cache: safe to evict globally on logout.
            db.weeklyLeaderboardDao().clear()
        }
        stagedMedia.distinct().forEach { path -> runCatching { File(path).delete() } }
    }
}
