package com.autodrive.app.feature.auth.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SudanPhoneNumberTest {

    @Test
    fun `normalizes local mobile number`() {
        assertEquals("249912345678", SudanPhoneNumber.normalize("0912345678"))
    }

    @Test
    fun `accepts plus country code`() {
        assertEquals("249912345678", SudanPhoneNumber.normalize("+249 91 234 5678"))
    }

    @Test
    fun `accepts international 00 prefix`() {
        assertEquals("249912345678", SudanPhoneNumber.normalize("00249 912345678"))
    }

    @Test
    fun `accepts Arabic Indic digits`() {
        assertEquals("249912345678", SudanPhoneNumber.normalize("٠٩١٢٣٤٥٦٧٨"))
    }

    @Test
    fun `accepts Persian digits`() {
        assertEquals("249912345678", SudanPhoneNumber.normalize("۰۹۱۲۳۴۵۶۷۸"))
    }

    @Test
    fun `rejects missing country or local prefix`() {
        assertNull(SudanPhoneNumber.normalize("912345678"))
    }

    @Test
    fun `rejects incorrect length`() {
        assertNull(SudanPhoneNumber.normalize("091234567"))
        assertNull(SudanPhoneNumber.normalize("09123456789"))
    }

    @Test
    fun `rejects blank input`() {
        assertNull(SudanPhoneNumber.normalize("   "))
    }
}
