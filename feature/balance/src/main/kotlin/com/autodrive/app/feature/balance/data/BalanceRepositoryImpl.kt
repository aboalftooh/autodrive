package com.autodrive.app.feature.balance.data

import androidx.room.withTransaction
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.MarketerBalanceEntity
import com.autodrive.app.core.database.entities.PendingOperationEntity
import com.autodrive.app.core.database.entities.WithdrawalRequestEntity
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.network.dto.BalanceActivityDto
import com.autodrive.app.core.network.dto.BalanceActivityPageParams
import com.autodrive.app.core.network.dto.RequestWithdrawalParams
import com.autodrive.app.core.network.serialization.BigDecimalSerializer
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.data.SyncScope
import com.autodrive.app.core.sync.outbox.OUTBOX_CONTRACT_VERSION
import com.autodrive.app.core.sync.outbox.OutboxEntityType
import com.autodrive.app.core.sync.outbox.OutboxOperationType
import com.autodrive.app.feature.balance.domain.model.BalanceActivityPage
import com.autodrive.app.feature.balance.domain.model.BalanceActivityPageCursor
import com.autodrive.app.feature.balance.domain.model.BalanceTransaction
import com.autodrive.app.feature.balance.domain.model.MarketerBalance
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.balance.domain.model.WithdrawalRequest
import com.autodrive.app.feature.balance.domain.model.WithdrawalStatus
import com.autodrive.app.feature.balance.domain.model.WithdrawalSubmitResult
import com.autodrive.app.feature.balance.domain.repository.BalanceRepository
import com.autodrive.app.core.model.money.Money
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceRepositoryImpl @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val db: AutoDriveDatabase,
    private val sessionReader: SessionReader
) : BalanceRepository {
    private val opJson = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override fun observeBalance(userId: String): Flow<MarketerBalance> {
        if (userId.isBlank()) return flowOf(MarketerBalance(Money.ZERO, ""))

        // Room stays the UI cache, but every observer heals it from the canonical booked balance.
        // The refresh runs concurrently so cached/offline UI is not blocked by the network.
        return channelFlow {
            launch(Dispatchers.IO) {
                runCatching { refreshCanonicalBalanceCache(userId) }
            }
            db.marketerBalanceDao().observe(userId)
                .map { it?.toDomain() ?: MarketerBalance(Money.ZERO, "") }
                .collect { send(it) }
        }.flowOn(Dispatchers.IO)
    }

    private suspend fun refreshCanonicalBalanceCache(userId: String) {
        val scope = SyncScope.from(sessionReader.currentSession()) ?: return
        if (scope.userId != userId) return

        val row = supabase.client.postgrest["marketer_balance"]
            .select(Columns.ALL) {
                filter {
                    eq("client_id", scope.clientId)
                    eq("org_id", scope.orgId)
                }
                limit(1)
            }
            .decodeList<CanonicalBalanceRowDto>()
            .singleOrNull() ?: return

        check(row.clientId == scope.clientId && row.orgId == scope.orgId) { "REMOTE_SCOPE_MISMATCH" }
        check(SyncScope.from(sessionReader.currentSession()) == scope) { "STALE_LOCAL_MUTATION_SCOPE" }

        db.marketerBalanceDao().upsert(
            MarketerBalanceEntity(
                id = row.id,
                userId = scope.userId,
                clientId = row.clientId,
                balance = row.balance,
                pendingWithdrawal = BigDecimal.ZERO,
                updatedAt = row.updatedAt,
            )
        )
    }

    override fun observeTransactions(userId: String, clientId: String): Flow<List<BalanceTransaction>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return db.balanceTransactionDao().observe(userId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getBalanceActivityPage(
        cursor: BalanceActivityPageCursor?,
        limit: Int,
    ): BalanceActivityPage = withContext(Dispatchers.IO) {
        val safeLimit = limit.coerceIn(1, 50)
        val rows = supabase.client.postgrest
            .rpc(
                "autodrive_balance_activity_page_v1",
                buildJsonObject {
                    put("p_limit", (safeLimit + 1).coerceAtMost(50))
                    put("p_before_created_at", cursor?.createdAt)
                    put("p_before_id", cursor?.id)
                },
            )
            .decodeList<BalanceActivityDto>()

        val hasMore = rows.size > safeLimit
        val pageRows = rows.take(safeLimit)
        val entries = pageRows.map { it.toDomain() }
        val nextCursor = entries.lastOrNull()?.let {
            BalanceActivityPageCursor(createdAt = it.createdAt, id = it.id)
        }
        BalanceActivityPage(entries = entries, nextCursor = nextCursor, hasMore = hasMore)
    }

    override fun observeWithdrawalRequests(userId: String): Flow<List<WithdrawalRequest>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return db.withdrawalRequestDao().observe(userId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    // v68: intent المحلي وطلب السحب الدائم يلتزمان أو يتراجعان معاً قبل أي RPC.
    override suspend fun requestWithdrawal(amount: Money, note: String?, clientId: String): Result<WithdrawalSubmitResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val scope = SyncScope.from(sessionReader.currentSession())
                    ?: return@runCatching Result.Error("المستخدم غير مسجّل")
                if (clientId != scope.clientId) {
                    return@runCatching Result.Error("جلسة العميل غير متطابقة")
                }
                val userEntity = db.autoDriveUserDao().get(scope.userId)
                if (
                    userEntity == null ||
                    userEntity.clientId != scope.clientId ||
                    userEntity.orgId != scope.orgId
                ) {
                    return@runCatching Result.Error("بيانات الحساب غير متطابقة")
                }
                if (userEntity.bankName.isNullOrBlank() || userEntity.bankAccount.isNullOrBlank()) {
                    return@runCatching Result.Error("يرجى إضافة بيانات الحساب البنكي من الملف الشخصي أولاً")
                }

                val clientRequestId = UUID.randomUUID().toString()
                val params = RequestWithdrawalParams(
                    amount = amount.amount,
                    note = note?.take(200)?.ifBlank { "" } ?: "",
                    clientRequestId = clientRequestId,
                )
                val nowIso = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    .format(Instant.now().atOffset(ZoneOffset.UTC))
                val localEntity = WithdrawalRequestEntity(
                    id = clientRequestId,
                    userId = scope.userId,
                    clientId = scope.clientId,
                    amount = amount.amount,
                    status = "PENDING",
                    bankName = userEntity.bankName.orEmpty(),
                    bankAccount = userEntity.bankAccount.orEmpty(),
                    note = note?.ifBlank { null },
                    createdAt = nowIso,
                    completedAt = null,
                    syncStatus = "PENDING_SYNC",
                )

                db.withTransaction {
                    check(SyncScope.from(sessionReader.currentSession()) == scope) { "STALE_LOCAL_MUTATION_SCOPE" }
                    db.withdrawalRequestDao().upsert(localEntity)
                    db.pendingOperationDao().insert(
                        PendingOperationEntity(
                            id = clientRequestId,
                            mutationId = clientRequestId,
                            userId = scope.userId,
                            clientId = scope.clientId,
                            orgId = scope.orgId,
                            entityType = OutboxEntityType.WITHDRAWAL,
                            entityId = clientRequestId,
                            operation = OutboxOperationType.REQUEST_WITHDRAWAL_RPC,
                            payload = opJson.encodeToString(params),
                            contractVersion = OUTBOX_CONTRACT_VERSION,
                        )
                    )
                }

                // Delivery authority is the scoped Outbox. The normal sync/worker may send it immediately or later.
                Result.Success(WithdrawalSubmitResult.PendingLocal)
            }.getOrElse { Result.Error(it.message ?: "تعذّر حفظ طلب السحب محليًا", it) }
        }

    // v69: steady-state withdrawal delivery is receipt-driven in OutboxSynchronizer.
    // Legacy text parsing/reconciliation helpers were removed from this repository.

    override suspend fun cancelAllPendingWithdrawals(): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val scope = SyncScope.from(sessionReader.currentSession())
                    ?: return@runCatching Result.Error("المستخدم غير مسجّل")
                val mutationId = UUID.randomUUID().toString()
                val receipt = supabase.client.postgrest
                    .rpc(
                        "autodrive_cancel_pending_withdrawals_command_v1",
                        buildJsonObject { put("p_mutation_id", mutationId) },
                    )
                    .decodeAs<CancelPendingWithdrawalsReceipt>()
                check(receipt.mutationId == mutationId) { "INVALID_SERVER_RECEIPT_MUTATION" }
                check(receipt.commandType == "CANCEL_PENDING_WITHDRAWALS") { "INVALID_SERVER_RECEIPT_COMMAND" }
                check(receipt.revisionKind == "COMMAND_RECEIPT" && receipt.serverRevision > 0L) {
                    "INVALID_SERVER_RECEIPT_REVISION"
                }
                check(receipt.resultStatus == "APPLIED") { receipt.errorCode ?: "CANCEL_PENDING_WITHDRAWALS_REJECTED" }
                check(SyncScope.from(sessionReader.currentSession()) == scope) { "STALE_LOCAL_MUTATION_SCOPE" }
                db.withdrawalRequestDao().deletePendingByUserId(scope.userId)
                Result.Success(receipt.resultCount ?: 0)
            }.getOrElse { Result.Error(it.message ?: "خطأ في إلغاء الطلبات", it) }
        }

    // ── Mappers ──────────────────────────────────────────────

    private fun BalanceActivityDto.toDomain() = BalanceTransaction(
        id = id,
        type = type,
        amount = Money.of(amount),
        description = note.orEmpty(),
        createdAt = createdAt,
        referenceType = referenceType,
        referenceId = referenceId,
        invoiceNumber = invoiceNumber,
    )

    private fun com.autodrive.app.core.database.entities.MarketerBalanceEntity.toDomain() = MarketerBalance(
        balance   = Money.of(balance),
        updatedAt = updatedAt
    )

    private fun com.autodrive.app.core.database.entities.BalanceTransactionEntity.toDomain() = BalanceTransaction(
        id          = id,
        type        = type,
        amount      = Money.of(amount),
        description = description,
        createdAt   = createdAt
    )

    private fun com.autodrive.app.core.database.entities.WithdrawalRequestEntity.toDomain() = WithdrawalRequest(
        id             = id,
        amount         = Money.of(amount),
        status         = runCatching { WithdrawalStatus.valueOf(status) }.getOrDefault(WithdrawalStatus.PENDING),
        bankName       = bankName,
        bankAccount    = bankAccount,
        transactionRef = transactionRef,
        note           = note,
        createdAt      = createdAt,
        completedAt    = completedAt
    )
}

@Serializable
private data class CanonicalBalanceRowDto(
    val id: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("org_id") val orgId: String,
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
private data class CancelPendingWithdrawalsParams(
    @SerialName("p_mutation_id") val mutationId: String,
)

@Serializable
private data class CancelPendingWithdrawalsReceipt(
    @SerialName("mutation_id") val mutationId: String,
    @SerialName("command_type") val commandType: String,
    @SerialName("result_status") val resultStatus: String,
    @SerialName("server_revision") val serverRevision: Long,
    @SerialName("revision_kind") val revisionKind: String,
    @SerialName("result_count") val resultCount: Int? = null,
    @SerialName("error_code") val errorCode: String? = null,
)

