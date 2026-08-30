package com.autodrive.app.feature.auth.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SudanPhoneNumberTest {
    @Test
    fun `normalizes supported Sudan phone formats`() {
        val expected = "249123456789"

        assertEquals(expected, SudanPhoneNumber.normalize("249123456789"))
        assertEquals(expected, SudanPhoneNumber.normalize("+249123456789"))
        assertEquals(expected, SudanPhoneNumber.normalize("00249123456789"))
        assertEquals(expected, SudanPhoneNumber.normalize("0123456789"))
    }

    @Test
    fun `rejects invalid Sudan phone formats`() {
        assertNull(SudanPhoneNumber.normalize(""))
        assertNull(SudanPhoneNumber.normalize("123456789"))
        assertNull(SudanPhoneNumber.normalize("24912345678"))
        assertNull(SudanPhoneNumber.normalize("002491234567890"))
    }
}
