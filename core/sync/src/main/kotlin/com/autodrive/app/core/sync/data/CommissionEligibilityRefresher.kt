package com.autodrive.app.core.sync.data

import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.CommissionEligibilityCacheEntity
import com.autodrive.app.core.database.entities.CommissionEligibilitySyncStateEntity
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.network.dto.EligibilityDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.time.Instant
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Materializes the server-authoritative commission_eligibility view into Room.
 * It is invoked by the shared sync engine, so APP_START, user refresh, FCM and
 * Realtime hints all converge through the same cache without UI polling.
 */
@Singleton
class CommissionEligibilityRefresher @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val db: AutoDriveDatabase,
) {
    suspend fun refresh(scope: SyncScope) {
        val rows = supabase.client.postgrest["commission_eligibility"]
            .select(Columns.ALL) { filter { eq("client_id", scope.clientId) } }
            .decodeList<EligibilityDto>()

        val weekStartMs = rows.firstOrNull()?.weekStart
            ?.let(::parseIsoMs)
            ?.takeIf { it > 0L }
            ?: fallbackLastFriday9Am()
        db.commissionEligibilityCacheDao().replaceSnapshot(
            clientId = scope.clientId,
            rows = rows.map { row ->
                CommissionEligibilityCacheEntity(
                    invoiceId = row.invoiceId,
                    clientId = scope.clientId,
                    commission = row.commission,
                    invoiceNumber = row.invoiceNumber,
                    createdAt = row.createdAt,
                    eligibility = row.eligibility,
                    paidOutAmount = row.paidOutAmount ?: java.math.BigDecimal.ZERO,
                    remainingAmount = row.remainingAmount,
                    withdrawableAmount = row.withdrawableAmount,
                    pendingAmount = row.pendingAmount,
                    creditRemainingAmount = row.creditRemainingAmount,
                    reasonCode = row.reasonCode,
                    reasonMessage = row.reasonMessage,
                )
            },
            state = CommissionEligibilitySyncStateEntity(
                clientId = scope.clientId,
                weekStartMs = weekStartMs,
                syncedAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun parseIsoMs(value: String): Long = runCatching {
        Instant.parse(value).toEpochMilli()
    }.getOrDefault(0L)

    private fun fallbackLastFriday9Am(): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"))
        val daysBack = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.FRIDAY -> if (calendar.get(Calendar.HOUR_OF_DAY) >= 9) 0 else 7
            Calendar.SATURDAY -> 1
            Calendar.SUNDAY -> 2
            Calendar.MONDAY -> 3
            Calendar.TUESDAY -> 4
            Calendar.WEDNESDAY -> 5
            Calendar.THURSDAY -> 6
            else -> 0
        }
        calendar.add(Calendar.DAY_OF_YEAR, -daysBack)
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
