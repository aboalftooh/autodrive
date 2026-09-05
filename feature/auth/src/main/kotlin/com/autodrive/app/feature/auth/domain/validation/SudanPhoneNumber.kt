package com.autodrive.app.feature.auth.domain.validation

object SudanPhoneNumber {
    private const val COUNTRY_CODE = "249"
    private const val INTERNATIONAL_LENGTH = 12

    fun normalize(input: String): String? {
        val digits = input.trim()
            .mapNotNull { it.toEnglishDigitOrNull() }
            .joinToString("")
        if (digits.isBlank()) return null

        val normalized = when {
            digits.startsWith("00249") -> digits.removePrefix("00")
            digits.startsWith(COUNTRY_CODE) -> digits
            digits.startsWith("0") -> COUNTRY_CODE + digits.drop(1)
            else -> return null
        }

        return normalized.takeIf {
            it.length == INTERNATIONAL_LENGTH && it.startsWith(COUNTRY_CODE)
        }
    }

    private fun Char.toEnglishDigitOrNull(): Char? = when (this) {
        in '0'..'9' -> this
        in '\u0660'..'\u0669' -> '0' + (code - '\u0660'.code)
        in '\u06F0'..'\u06F9' -> '0' + (code - '\u06F0'.code)
        else -> null
    }
}
