package com.autodrive.app.core.model.money

import com.autodrive.app.core.model.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.RoundingMode

class MoneyTest {

    @Test
    fun addition_isDecimalExact() {
        assertEquals(Money.of("0.3"), Money.of("0.1") + Money.of("0.2"))
    }

    @Test
    fun subtraction_isDecimalExact() {
        assertEquals(Money.of("999999999999999999.99"), Money.of("1000000000000000000.00") - Money.of("0.01"))
    }

    @Test
    fun equality_ignoresBigDecimalScale() {
        assertEquals(Money.of("10.0"), Money.of("10.00"))
    }

    @Test
    fun rounding_isExplicitAndHalfUp() {
        assertEquals(Money.of("12.35"), Money.of("12.345").rounded(2, RoundingMode.HALF_UP))
    }

    @Test
    fun mixedCurrencies_areRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Money.of("1", "SAR") + Money.of("1", "USD")
        }
    }
}
