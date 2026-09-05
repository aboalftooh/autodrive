package com.autodrive.app.feature.commission.domain

import com.autodrive.app.feature.commission.domain.model.CommissionEntry
import com.autodrive.app.feature.commission.domain.model.CommissionStatus
import com.autodrive.app.feature.commission.domain.model.CommissionSummary
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import com.autodrive.app.core.model.money.Money
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommissionCalculator @Inject constructor() {
    /** حد أسبوع محلي احتياطي للعرض فقط؛ أهلية العمولة يحددها السيرفر. */
    fun fallbackLastFriday9AM(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"))
        val daysBack = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.FRIDAY   -> if (cal.get(Calendar.HOUR_OF_DAY) >= 9) 0 else 7
            Calendar.SATURDAY -> 1
            Calendar.SUNDAY   -> 2
            Calendar.MONDAY   -> 3
            Calendar.TUESDAY  -> 4
            Calendar.WEDNESDAY -> 5
            Calendar.THURSDAY -> 6
            else -> 0
        }
        cal.add(Calendar.DAY_OF_YEAR, -daysBack)
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    /** موعد محلي احتياطي للعد التنازلي فقط؛ لا يقرر الأهلية المالية. */
    fun fallbackNextFriday9AM(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"))
        val daysAhead = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.FRIDAY   -> if (cal.get(Calendar.HOUR_OF_DAY) < 9) 0 else 7
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY   -> 5
            Calendar.MONDAY   -> 4
            Calendar.TUESDAY  -> 3
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 1
            else -> 7
        }
        cal.add(Calendar.DAY_OF_YEAR, daysAhead)
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // يحلّل ISO 8601 مع أو بدون timezone (Supabase يُرسل UTC مع +00:00)
    fun parseIsoMs(iso: String): Long {
        if (iso.isEmpty()) return 0L
        return runCatching {
            Instant.parse(iso).toEpochMilli()
        }.recoverCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(iso)?.time ?: 0L
        }.getOrDefault(0L)
    }

    fun fallbackLastFriday9AMLabel(): String {
        val ms = fallbackLastFriday9AM()
        val fmt = SimpleDateFormat("dd/MM", Locale.getDefault())
        return "الجمعة ${fmt.format(ms)} في 9:00 ص"
    }

    // 5.5: حُذف منطق الأهلية المحلي (isEligible/calculate) نهائياً — مصدر الحقيقة الوحيد
    // هو view commission_eligibility عبر CommissionRepository.getEligibilities(clientId).
    // ما تبقى هنا أدوات حدود الأسبوع (fallbackLastFriday9AM/fallbackNextFriday9AM)، تحليل التاريخ، وتجميع صفوف
    // مُصنَّفة سلفاً من السيرفر (summarize) — وليست تصنيف أهلية.

    // weekStartMs: server-authoritative last_friday_9am() in ms — 0 uses display-only local fallback
    fun summarize(entries: List<CommissionEntry>, weekStartMs: Long = 0L): CommissionSummary {
        val cutoff = if (weekStartMs > 0L) weekStartMs else fallbackLastFriday9AM()
        val weekEnd = cutoff + 7L * 24 * 3_600_000L

        fun List<CommissionEntry>.moneySum() = Money.sum(map { it.amount })

        val withdrawable = entries.filter { it.status == CommissionStatus.WITHDRAWABLE }.moneySum()
        val pending      = entries.filter { it.status == CommissionStatus.PENDING }.moneySum()
        val paid         = entries.filter { it.status == CommissionStatus.PAID }.moneySum()
        val weeklyTotal  = entries
            .filter {
                it.status != CommissionStatus.PAID &&
                    parseIsoMs(it.createdAt) in cutoff until weekEnd
            }
            .moneySum()

        return CommissionSummary(
            withdrawable       = withdrawable,
            pending            = pending,
            paid               = paid,
            weeklyTotal        = weeklyTotal,
            lastFriday9AmLabel = fallbackLastFriday9AMLabel(),
            weekStartMs        = cutoff
        )
    }
}
