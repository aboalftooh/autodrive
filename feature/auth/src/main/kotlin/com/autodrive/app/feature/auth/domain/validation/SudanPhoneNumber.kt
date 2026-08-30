package com.autodrive.app.feature.auth.domain.validation

object SudanPhoneNumber {
    private const val COUNTRY_CODE = "249"
    private const val INTERNATIONAL_LENGTH = 12

    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        val digits = trimmed.filter { it.isDigit() }
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
}
