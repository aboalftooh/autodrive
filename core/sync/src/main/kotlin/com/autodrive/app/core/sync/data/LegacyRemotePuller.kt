package com.autodrive.app.core.sync.data

import androidx.room.withTransaction
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.network.dto.AutoDriveUserDto
import com.autodrive.app.core.network.dto.BalanceTransactionDto
import com.autodrive.app.core.network.dto.CommissionPaymentDto
import com.autodrive.app.core.network.dto.InvoiceDto
import com.autodrive.app.core.network.dto.MarketerBalanceDto
import com.autodrive.app.core.network.dto.NotificationDto
import com.autodrive.app.core.network.dto.PaymentDto
import com.autodrive.app.core.network.dto.WithdrawalRequestDto
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.diagnostics.SyncDiagnostics
import com.autodrive.app.core.sync.domain.SyncPhase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compatibility positive-row pulls. These are snapshots, not canonical event streams:
 * no eventId/serverRevision is synthesized and absence is never interpreted as deletion.
 */
@Singleton
class LegacyRemotePuller @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val db: AutoDriveDatabase,
    private val guard: PendingLocalMutationGuard,
    private val sessionReader: SessionReader,
    private val diagnostics: SyncDiagnostics,
    private val chatRecovery: ChatRecoverySynchronizer,
) {
    suspend fun pull(scope: SyncScope, onPhase: suspend (SyncPhase) -> Unit): SyncEngineResult {
        val steps = SyncStepExecutor(onPhase, diagnostics)
        val postgrest = supabase.client.postgrest

        steps.run(SyncPhase.PROFILE) {
            val remote = postgrest["autodrive_users"]
                .select(Columns.ALL) { filter { eq("user_id", scope.userId) } }
                .decodeSingleOrNull<AutoDriveUserDto>()
            if (remote != null) {
                val merged = guard.profile(scope, remote)
                snapshotTransaction(scope) {
                    // A user can be re-linked to another client/org. Remove stale local rows first so
                    // downstream account-sensitive reads never resolve an arbitrary historic profile.
                    db.autoDriveUserDao().deleteByUserId(scope.userId)
                    db.autoDriveUserDao().upsert(merged)
                }
            }
        }

        // Invoices and payments are fetched fully before either set is written, then committed together.
        steps.run(SyncPhase.INVOICES) { pullBillingSnapshot(scope) }
        // PAYMENTS is a logical phase retained for diagnostics; its apply was committed with INVOICES above.
        steps.run(SyncPhase.PAYMENTS) { Unit }

        steps.run(SyncPhase.COMMISSIONS) {
            val rows = postgrest["commission_payments"].select(Columns.ALL) {
                filter { eq("client_id", scope.clientId) }
            }.decodeList<CommissionPaymentDto>()
            rows.forEach { guard.requireClient(scope, it.clientId) }
            val entities = rows.distinctBy { it.id }.map { it.toEntity() }
            snapshotTransaction(scope) { db.commissionPaymentDao().upsertAll(entities) }
        }

        steps.run(SyncPhase.BALANCE) {
            val dto = postgrest["marketer_balance"].select(Columns.ALL) {
                filter { eq("client_id", scope.clientId) }
                limit(1)
            }.decodeSingleOrNull<MarketerBalanceDto>()
            if (dto != null) {
                guard.requireClientOrg(scope, dto.clientId, dto.orgId)
                snapshotTransaction(scope) {
                    db.marketerBalanceDao().deleteByUserId(scope.userId)
                    db.marketerBalanceDao().upsert(dto.toEntity(scope.userId))
                }
            }
        }

        steps.run(SyncPhase.TRANSACTIONS) {
            val rows = postgrest["balance_transactions"].select(Columns.ALL) {
                filter { eq("client_id", scope.clientId) }
                order("created_at", Order.DESCENDING)
                limit(50)
            }.decodeList<BalanceTransactionDto>()
            snapshotTransaction(scope) {
                val merged = rows.distinctBy { it.id }.map { guard.balanceTransaction(scope, it) }
                db.balanceTransactionDao().upsertAll(merged)
            }
        }

        steps.run(SyncPhase.WITHDRAWALS) {
            val rows = postgrest["withdrawal_requests"].select(Columns.ALL) {
                filter { eq("client_id", scope.clientId) }
                order("requested_at", Order.DESCENDING)
                limit(20)
            }.decodeList<WithdrawalRequestDto>()
            snapshotTransaction(scope) {
                val pending = rows.distinctBy { it.id }.mapNotNull { guard.withdrawal(scope, it) }
                db.withdrawalRequestDao().upsertAll(pending)
            }
        }

        steps.run(SyncPhase.NOTIFICATIONS) {
            val rows = postgrest["notifications"].select(Columns.ALL) {
                filter { eq("user_id", scope.userId) }
                order("created_at", Order.DESCENDING)
                limit(50)
            }.decodeList<NotificationDto>()
            snapshotTransaction(scope) {
                val merged = rows.distinctBy { it.id }.map { guard.notification(scope, it) }
                db.notificationDao().upsertAll(merged)
            }
        }

        steps.run(SyncPhase.CHAT) { chatRecovery.recover(scope) }
        return SyncEngineResult(steps.completedPhases, steps.failures)
    }

    private suspend fun pullBillingSnapshot(scope: SyncScope) {
        val postgrest = supabase.client.postgrest
        val invoices = postgrest["invoices"].select(Columns.ALL) {
            filter {
                eq("client_id", scope.clientId)
                eq("category", "SALE")
                gt("commission", 0)
            }
        }.decodeList<InvoiceDto>()
        invoices.forEach { guard.requireClient(scope, it.clientId) }

        // No local invoice writes occur until this second network fetch also succeeds.
        val payments = postgrest["payments"].select(Columns.ALL) {
            filter { eq("client_id", scope.clientId) }
        }.decodeList<PaymentDto>()
        payments.forEach { guard.requireClient(scope, it.clientId) }

        val invoiceEntities = invoices.distinctBy { it.id }.map { it.toEntity() }
        val paymentEntities = payments.distinctBy { it.id }.map { it.toEntity() }
        snapshotTransaction(scope) {
            db.invoiceDao().upsertAll(invoiceEntities)
            db.paymentDao().upsertAll(paymentEntities)
        }
    }

    private suspend inline fun snapshotTransaction(
        scope: SyncScope,
        crossinline block: suspend () -> Unit,
    ) {
        db.withTransaction {
            if (SyncScope.from(sessionReader.currentSession()) != scope) throw StaleSyncScopeException()
            block()
        }
    }
}
