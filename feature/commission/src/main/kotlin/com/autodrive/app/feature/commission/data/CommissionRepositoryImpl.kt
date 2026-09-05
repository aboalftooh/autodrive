package com.autodrive.app.feature.commission.data

import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.InvoiceEntity
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.observability.AppLogger
import com.autodrive.app.core.network.dto.EligibilityDto
import com.autodrive.app.core.network.dto.InvoiceItemDto
import com.autodrive.app.feature.commission.domain.model.InvoiceItem
import com.autodrive.app.feature.commission.domain.CommissionCalculator
import com.autodrive.app.feature.commission.domain.model.CommissionEntry
import com.autodrive.app.feature.commission.domain.model.CommissionStatus
import com.autodrive.app.feature.commission.domain.model.CommissionSummary
import com.autodrive.app.feature.commission.domain.model.Invoice
import com.autodrive.app.feature.commission.domain.model.InvoiceCategory
import com.autodrive.app.feature.commission.domain.model.InvoiceStatus
import com.autodrive.app.feature.commission.domain.repository.CommissionRepository
import com.autodrive.app.core.model.money.Money
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommissionRepositoryImpl @Inject constructor(
    private val calculator: CommissionCalculator,
    private val supabase: AutoDriveSupabase,
    private val db: AutoDriveDatabase
) : CommissionRepository {

    override fun observeCommissions(clientId: String): Flow<Pair<CommissionSummary, List<CommissionEntry>>> {
        // Changes in the local cache, the Friday boundary, and a small periodic
        // retry all refresh the server-authoritative snapshot. A failed request
        // must not terminate the Flow and leave the reports screen at zero.
        return merge(
            db.invoiceDao().observeByClientId(clientId).map { Unit },
            weekBoundaryChanges(),
            periodicRefreshes(),
        )
            .onStart { emit(Unit) }
            .map {
                runCatching { loadCommissionSnapshot(clientId) }
                    .onFailure { error ->
                        AppLogger.e(TAG, "Commission snapshot refresh failed", error)
                    }
                    .getOrElse { loadLocalCommissionSnapshot(clientId) }
            }
            .flowOn(Dispatchers.IO)
    }

    @Suppress("DEPRECATION")
    private suspend fun loadCommissionSnapshot(
        clientId: String
    ): Pair<CommissionSummary, List<CommissionEntry>> {
        val dtos = supabase.client.postgrest["commission_eligibility"]
            .select(Columns.ALL) { filter { eq("client_id", clientId) } }
            .decodeList<EligibilityDto>()
        val weekStartMs = dtos.firstOrNull()
            ?.weekStart
            ?.let { calculator.parseIsoMs(it) }
            ?: calculator.fallbackLastFriday9AM()
        val entries = dtos.mapNotNull { it.toCommissionEntry() }
            .sortedByDescending { it.createdAt }
        return Pair(calculator.summarize(entries, weekStartMs), entries)
    }

    @Suppress("DEPRECATION")
    private fun weekBoundaryChanges(): Flow<Unit> = flow {
        while (true) {
            val waitMs = (calculator.fallbackNextFriday9AM() - System.currentTimeMillis())
                .coerceAtLeast(1_000L)
            delay(waitMs)
            emit(Unit)
            delay(1_000L)
        }
    }

    private fun periodicRefreshes(): Flow<Unit> = flow {
        while (true) {
            delay(SNAPSHOT_RETRY_MS)
            emit(Unit)
        }
    }

    /**
     * Display-only fallback for a temporary View/API failure. It keeps the
     * report useful from the already synchronized invoice cache; all normal
     * reads still use commission_eligibility as the source of truth.
     */
    private suspend fun loadLocalCommissionSnapshot(
        clientId: String,
    ): Pair<CommissionSummary, List<CommissionEntry>> {
        val invoices = db.invoiceDao().getByClientId(clientId)
        val paymentsByInvoice = if (invoices.isEmpty()) {
            emptyMap()
        } else {
            db.paymentDao()
                .getByInvoiceIds(invoices.map { it.id })
                .groupingBy { it.invoiceId }
                .fold(BigDecimal.ZERO) { total, payment -> total + payment.amount }
        }
        val cutoff = calculator.fallbackLastFriday9AM()
        val entries = invoices.map { invoice ->
            val createdMs = calculator.parseIsoMs(invoice.createdAt)
            val paidEnough = paymentsByInvoice[invoice.id]?.let { it >= invoice.totalAmount } == true
            val status = when {
                invoice.status.equals("CLOSED_CASH", ignoreCase = true) && createdMs < cutoff ->
                    CommissionStatus.WITHDRAWABLE
                invoice.status.equals("CLOSED_CREDIT", ignoreCase = true) &&
                    createdMs < cutoff && paidEnough -> CommissionStatus.WITHDRAWABLE
                else -> CommissionStatus.PENDING
            }
            CommissionEntry(
                invoiceId = invoice.id,
                invoiceNumber = invoice.invoiceNumber,
                amount = Money.of(invoice.commission),
                status = status,
                createdAt = invoice.createdAt,
            )
        }.sortedByDescending { it.createdAt }
        return calculator.summarize(entries, cutoff) to entries
    }

    override fun observeInvoices(clientId: String): Flow<List<Invoice>> =
        db.invoiceDao().observeByClientId(clientId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    // ── Entity → Domain mappers ──────────────────────────────

    private fun InvoiceEntity.toDomain() = Invoice(
        id            = id,
        clientId      = clientId,
        commission    = Money.of(commission),
        status        = runCatching { InvoiceStatus.valueOf(status.uppercase()) }.getOrDefault(InvoiceStatus.OPEN),
        category      = runCatching { InvoiceCategory.valueOf(category.uppercase()) }.getOrDefault(InvoiceCategory.OTHER),
        totalAmount   = Money.of(totalAmount),
        invoiceNumber = invoiceNumber,
        createdAt     = createdAt
    )

    // FIX-7: يستعلم من commission_eligibility view — السيرفر مصدر الحقيقة للأهلية
    override suspend fun getEligibilities(clientId: String): List<CommissionEntry> =
        withContext(Dispatchers.IO) {
            supabase.client.postgrest["commission_eligibility"]
                .select(Columns.ALL) { filter { eq("client_id", clientId) } }
                .decodeList<EligibilityDto>()
                .mapNotNull { it.toCommissionEntry() }
                .sortedByDescending { it.createdAt }
        }

    override suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItem> =
        withContext(Dispatchers.IO) {
            supabase.client.postgrest["invoice_items"]
                .select(Columns.ALL) { filter { eq("invoice_id", invoiceId) } }
                .decodeList<InvoiceItemDto>()
                .map { it.toDomain() }
        }

    private fun InvoiceItemDto.toDomain() = InvoiceItem(
        id          = id,
        itemName    = itemName,
        itemType    = itemType,
        description = description,
        quantity    = quantity,
        sellPrice   = Money.of(sellPrice),
        totalPrice  = Money.of(totalPrice)
    )

    private fun EligibilityDto.toCommissionEntry(): CommissionEntry? {
        val status = when (eligibility) {
            "PAID"         -> CommissionStatus.PAID
            "WITHDRAWABLE" -> CommissionStatus.WITHDRAWABLE
            "PENDING"      -> CommissionStatus.PENDING
            else           -> return null
        }
        return CommissionEntry(
            invoiceId     = invoiceId,
            invoiceNumber = invoiceNumber,
            amount        = Money.of(commission),
            status        = status,
            createdAt     = createdAt
        )
    }

    private companion object {
        const val TAG = "CommissionRepository"
        const val SNAPSHOT_RETRY_MS = 30_000L
    }
}
