package com.autodrive.app.core.model.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * قيمة مالية دقيقة. لا تستخدم [Double] في الحساب أو التخزين؛ التحويل إليه مسموح
 * فقط عند حدود العرض مثل الرسوم المتحركة والمخططات.
 */
class Money private constructor(
    val amount: BigDecimal,
    val currencyCode: String,
) : Comparable<Money> {

    init {
        require(currencyCode.matches(Regex("[A-Z]{3}"))) { "currencyCode must be ISO-4217" }
    }

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return of(amount.add(other.amount), currencyCode)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return of(amount.subtract(other.amount), currencyCode)
    }

    operator fun unaryMinus(): Money = of(amount.negate(), currencyCode)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amount.compareTo(other.amount)
    }

    fun abs(): Money = of(amount.abs(), currencyCode)

    fun rounded(scale: Int = DEFAULT_SCALE, mode: RoundingMode = RoundingMode.HALF_UP): Money =
        of(amount.setScale(scale, mode), currencyCode)

    fun isZero(): Boolean = amount.compareTo(BigDecimal.ZERO) == 0
    fun isPositive(): Boolean = amount.signum() > 0
    fun isNegative(): Boolean = amount.signum() < 0

    /** للاستخدام داخل Presentation فقط. */
    fun toDisplayDouble(): Double = amount.toDouble()

    fun toPlainString(): String = amount.toPlainString()

    override fun equals(other: Any?): Boolean =
        other is Money &&
            currencyCode == other.currencyCode &&
            amount.compareTo(other.amount) == 0

    override fun hashCode(): Int = 31 * currencyCode.hashCode() + amount.stripTrailingZeros().hashCode()

    override fun toString(): String = "${amount.toPlainString()} $currencyCode"

    private fun requireSameCurrency(other: Money) {
        require(currencyCode == other.currencyCode) {
            "Currency mismatch: $currencyCode != ${other.currencyCode}"
        }
    }

    companion object {
        const val DEFAULT_CURRENCY = "SAR"
        const val DEFAULT_SCALE = 2

        val ZERO: Money = of(BigDecimal.ZERO)

        fun of(amount: BigDecimal, currencyCode: String = DEFAULT_CURRENCY): Money =
            Money(amount.canonical(), currencyCode.uppercase())

        fun of(amount: String, currencyCode: String = DEFAULT_CURRENCY): Money =
            of(amount.toBigDecimal(), currencyCode)

        fun of(amount: Long, currencyCode: String = DEFAULT_CURRENCY): Money =
            of(BigDecimal.valueOf(amount), currencyCode)

        /** Boundary compatibility only; do not use for calculations. */
        fun fromLegacyDouble(amount: Double, currencyCode: String = DEFAULT_CURRENCY): Money =
            of(BigDecimal.valueOf(amount), currencyCode)

        fun sum(values: Iterable<Money>, currencyCode: String = DEFAULT_CURRENCY): Money =
            values.fold(of(BigDecimal.ZERO, currencyCode)) { total, value -> total + value }

        private fun BigDecimal.canonical(): BigDecimal =
            if (compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else stripTrailingZeros()
    }
}
