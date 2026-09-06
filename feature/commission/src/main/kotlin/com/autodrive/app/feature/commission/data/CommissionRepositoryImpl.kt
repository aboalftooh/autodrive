package com.autodrive.app.feature.commission.data

import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.CommissionEligibilityCacheEntity
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.network.dto.InvoiceItemDto
import com.autodrive.app.core.network.dto.EligibilityDto
import com.autodrive.app.core.network.dto.CommissionPageParams
import com.autodrive.app.core.network.dto.CommissionListSummaryDto
import com.autodrive.app.feature.commission.domain.CommissionCalculator
import com.autodrive.app.feature.commission.domain.model.CommissionEntry
import com.autodrive.app.feature.commission.domain.model.CommissionSnapshot
import com.autodrive.app.feature.commission.domain.model.CommissionSnapshotSource
import com.autodrive.app.feature.commission.domain.model.CommissionPage
import com.autodrive.app.feature.commission.domain.model.CommissionPageCursor
import com.autodrive.app.feature.commission.domain.model.CommissionListSummary
import com.autodrive.app.feature.commission.domain.model.CommissionStatus
import com.autodrive.app.feature.commission.domain.model.Invoice
import com.autodrive.app.feature.commission.domain.model.InvoiceCategory
import com.autodrive.app.feature.commission.domain.model.InvoiceItem
import com.autodrive.app.feature.commission.domain.model.InvoiceStatus
import com.autodrive.app.feature.commission.domain.repository.CommissionRepository
import com.autodrive.app.core.model.money.Money
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommissionRepositoryImpl @Inject constructor(
    private val calculator: CommissionCalculator,
    private val supabase: AutoDriveSupabase,
    private val db: AutoDriveDatabase,
) : CommissionRepository {

    /**
     * Financial SSOT: commission eligibility is never reconstructed from local invoices/payments.
     * Room only materializes the server-authoritative commission_eligibility snapshot.
     */
    override fun observeCommissions(clientId: String): Flow<CommissionSnapshot> {
        val cacheDao = db.commissionEligibilityCacheDao()
        return combine(
            cacheDao.observeByClientId(clientId),
            cacheDao.observeSyncState(clientId),
        ) { rows, state ->
            if (state == null) {
                CommissionSnapshot(
                    summary = calculator.summarize(emptyList()),
                    entries = emptyList(),
                    source = CommissionSnapshotSource.UNVERIFIED_EMPTY,
                )
            } else {
                val entries = rows.mapNotNull { it.toCommissionEntry() }
                    .sortedByDescending { it.createdAt }
                CommissionSnapshot(
                    summary = calculator.summarize(entries, state.weekStartMs),
                    entries = entries,
                    source = CommissionSnapshotSource.SERVER_CACHE,
                    lastSyncedAt = state.syncedAt,
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun observeInvoices(clientId: String): Flow<List<Invoice>> =
        db.invoiceDao().observeByClientId(clientId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    private fun com.autodrive.app.core.database.entities.InvoiceEntity.toDomain() = Invoice(
        id = id,
        clientId = clientId,
        commission = Money.of(commission),
        status = runCatching { InvoiceStatus.valueOf(status.uppercase()) }.getOrDefault(InvoiceStatus.OPEN),
        category = runCatching { InvoiceCategory.valueOf(category.uppercase()) }.getOrDefault(InvoiceCategory.OTHER),
        totalAmount = Money.of(totalAmount),
        invoiceNumber = invoiceNumber,
        createdAt = createdAt,
    )

    override suspend fun getCommissionPage(cursor: CommissionPageCursor?, limit: Int): CommissionPage =
        withContext(Dispatchers.IO) {
            val safeLimit = limit.coerceIn(1, 50)
            val rows = supabase.client.postgrest
                .rpc(
                    "autodrive_commission_page_v1",
                    CommissionPageParams(
                        limit = (safeLimit + 1).coerceAtMost(50),
                        beforeCreatedAt = cursor?.createdAt,
                        beforeInvoiceId = cursor?.invoiceId,
                    ),
                )
                .decodeList<EligibilityDto>()

            val hasMore = rows.size > safeLimit
            val pageRows = rows.take(safeLimit)
            val entries = pageRows.mapNotNull { it.toCommissionEntry() }
            val nextCursor = entries.lastOrNull()?.let {
                CommissionPageCursor(createdAt = it.createdAt, invoiceId = it.invoiceId)
            }
            CommissionPage(entries = entries, nextCursor = nextCursor, hasMore = hasMore)
        }

    override suspend fun getCommissionListSummary(): CommissionListSummary =
        withContext(Dispatchers.IO) {
            val dto = supabase.client.postgrest
                .rpc("autodrive_commission_summary_v1")
                .decodeList<CommissionListSummaryDto>()
                .firstOrNull() ?: CommissionListSummaryDto()
            CommissionListSummary(
                totalCount = dto.totalCount,
                totalAmount = Money.of(dto.totalAmount),
            )
        }

    override suspend fun getPendingCommissionPage(cursor: CommissionPageCursor?, limit: Int): CommissionPage =
        withContext(Dispatchers.IO) {
            val safeLimit = limit.coerceIn(1, 50)
            val rows = supabase.client.postgrest
                .rpc(
                    "autodrive_pending_commission_page_v1",
                    CommissionPageParams(
                        limit = (safeLimit + 1).coerceAtMost(50),
                        beforeCreatedAt = cursor?.createdAt,
                        beforeInvoiceId = cursor?.invoiceId,
                    ),
                )
                .decodeList<EligibilityDto>()

            val hasMore = rows.size > safeLimit
            val pageRows = rows.take(safeLimit)
            val entries = pageRows.mapNotNull { it.toCommissionEntry() }
            val nextCursor = entries.lastOrNull()?.let {
                CommissionPageCursor(createdAt = it.createdAt, invoiceId = it.invoiceId)
            }
            CommissionPage(entries = entries, nextCursor = nextCursor, hasMore = hasMore)
        }

    override suspend fun getPendingCommissionListSummary(): CommissionListSummary =
        withContext(Dispatchers.IO) {
            val dto = supabase.client.postgrest
                .rpc("autodrive_pending_commission_summary_v1")
                .decodeList<CommissionListSummaryDto>()
                .firstOrNull() ?: CommissionListSummaryDto()
            CommissionListSummary(
                totalCount = dto.totalCount,
                totalAmount = Money.of(dto.totalAmount),
            )
        }

    override suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItem> = withContext(Dispatchers.IO) {
        supabase.client.postgrest["invoice_items"]
            .select(Columns.ALL) { filter { eq("invoice_id", invoiceId) } }
            .decodeList<InvoiceItemDto>()
            .map { it.toDomain() }
    }

    private fun InvoiceItemDto.toDomain() = InvoiceItem(
        id = id,
        itemName = itemName,
        itemType = itemType,
        description = description,
        quantity = quantity,
        sellPrice = Money.of(sellPrice),
        totalPrice = Money.of(totalPrice),
    )

    private fun EligibilityDto.toCommissionEntry(): CommissionEntry? {
        val status = when (eligibility) {
            "PAID" -> CommissionStatus.PAID
            "WITHDRAWABLE" -> CommissionStatus.WITHDRAWABLE
            "PENDING" -> CommissionStatus.PENDING
            else -> return null
        }
        val paid = Money.of(paidOutAmount ?: java.math.BigDecimal.ZERO)
        val remaining = Money.of(remainingAmount)
        return CommissionEntry(
            invoiceId = invoiceId,
            invoiceNumber = invoiceNumber,
            amount = Money.of(commission),
            status = status,
            createdAt = createdAt,
            paidOutAmount = paid,
            remainingAmount = remaining,
            withdrawableAmount = Money.of(withdrawableAmount),
            pendingAmount = Money.of(pendingAmount),
            creditRemainingAmount = Money.of(creditRemainingAmount),
            reasonCode = reasonCode,
            reasonMessage = reasonMessage,
        )
    }

    private fun CommissionEligibilityCacheEntity.toCommissionEntry(): CommissionEntry? {
        val status = when (eligibility) {
            "PAID" -> CommissionStatus.PAID
            "WITHDRAWABLE" -> CommissionStatus.WITHDRAWABLE
            "PENDING" -> CommissionStatus.PENDING
            else -> return null
        }
        return CommissionEntry(
            invoiceId = invoiceId,
            invoiceNumber = invoiceNumber,
            amount = Money.of(commission),
            status = status,
            createdAt = createdAt,
            paidOutAmount = Money.of(paidOutAmount),
            remainingAmount = Money.of(remainingAmount),
            withdrawableAmount = Money.of(withdrawableAmount),
            pendingAmount = Money.of(pendingAmount),
            creditRemainingAmount = Money.of(creditRemainingAmount),
            reasonCode = reasonCode,
            reasonMessage = reasonMessage,
        )
    }
}
