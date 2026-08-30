package com.autodrive.app.core.common.format

import java.text.NumberFormat
import java.math.BigDecimal
import com.autodrive.app.core.model.money.Money
import java.text.SimpleDateFormat
import java.util.Locale

object FormatUtils {

    private val sarFormat = NumberFormat.getInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    /** تنسيق المبلغ دون تمريره عبر Double. */
    fun formatSar(amount: Money): String = sarFormat.format(amount.amount)

    fun formatSar(amount: BigDecimal): String = sarFormat.format(amount)

    /** مبلغ بدون لاحقة (للجداول) */
    fun formatSarNoLabel(amount: Money): String = sarFormat.format(amount.amount)

    fun formatSarNoLabel(amount: BigDecimal): String = sarFormat.format(amount)

    /** تنسيق التاريخ من ISO */
    fun formatDate(isoDate: String): String {
        return runCatching {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val parsed = input.parse(isoDate) ?: return isoDate
            output.format(parsed)
        }.getOrDefault(isoDate)
    }

    /** الحرف الأول من الاسم للأفاتار */
    fun initial(name: String): String {
        return name.trim().firstOrNull()?.toString() ?: "؟"
    }

    private val arabicMonths = listOf(
        "يناير","فبراير","مارس","أبريل","مايو","يونيو",
        "يوليو","أغسطس","سبتمبر","أكتوبر","نوفمبر","ديسمبر"
    )

    /** تنسيق تاريخ الانضمام: "انضم في مايو 2024" */
    fun formatJoinDate(isoDate: String): String {
        if (isoDate.isBlank()) return ""
        return runCatching {
            val input  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val parsed = input.parse(isoDate) ?: return ""
            val cal    = java.util.Calendar.getInstance().apply { time = parsed }
            val month  = arabicMonths[cal.get(java.util.Calendar.MONTH)]
            val year   = cal.get(java.util.Calendar.YEAR)
            "انضم في $month $year"
        }.getOrDefault("")
    }

    /** وقت فقط: "14:35" — لعرض آخر تحديث */
    fun formatTime(isoDate: String): String {
        if (isoDate.isBlank()) return ""
        return runCatching {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("HH:mm", Locale.getDefault())
            val parsed = input.parse(isoDate.substringBefore('+').substringBefore('Z')) ?: return isoDate
            output.format(parsed)
        }.getOrDefault("")
    }

    /** تنسيق قصير: "مايو 2024" */
    fun formatJoinDateShort(isoDate: String): String {
        if (isoDate.isBlank()) return ""
        return runCatching {
            val input  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val parsed = input.parse(isoDate) ?: return ""
            val cal    = java.util.Calendar.getInstance().apply { time = parsed }
            val month  = arabicMonths[cal.get(java.util.Calendar.MONTH)]
            val year   = cal.get(java.util.Calendar.YEAR)
            "$month $year"
        }.getOrDefault("")
    }
}
