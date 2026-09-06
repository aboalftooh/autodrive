package com.autodrive.app.feature.auth.presentation.register

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegistrationDraftStore private constructor(
    private val prefs: SharedPreferences?,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    )

    internal constructor() : this(null)

    private val memory = mutableMapOf<String, String>()

    var accountType: String
        get() = get(KEY_ACCOUNT_TYPE, "MARKETER")
        set(value) = set(KEY_ACCOUNT_TYPE, value)
    var fullName: String
        get() = get(KEY_FULL_NAME)
        set(value) = set(KEY_FULL_NAME, value)
    var phone: String
        get() = get(KEY_PHONE)
        set(value) = set(KEY_PHONE, value)
    var bankName: String
        get() = get(KEY_BANK_NAME)
        set(value) = set(KEY_BANK_NAME, value)
    var bankAccount: String
        get() = get(KEY_BANK_ACCOUNT)
        set(value) = set(KEY_BANK_ACCOUNT, value)
    var workshopName: String
        get() = get(KEY_WORKSHOP_NAME)
        set(value) = set(KEY_WORKSHOP_NAME, value)
    var specialty: String
        get() = get(KEY_SPECIALTY)
        set(value) = set(KEY_SPECIALTY, value)
    var workersCount: String
        get() = get(KEY_WORKERS_COUNT)
        set(value) = set(KEY_WORKERS_COUNT, value)
    var address: String
        get() = get(KEY_ADDRESS)
        set(value) = set(KEY_ADDRESS, value)

    fun clear() {
        memory.clear()
        prefs?.edit()?.clear()?.apply()
    }

    private fun get(key: String, default: String = ""): String =
        prefs?.getString(key, default) ?: memory[key] ?: default

    private fun set(key: String, value: String) {
        memory[key] = value
        prefs?.edit()?.putString(key, value)?.apply()
    }

    private companion object {
        const val PREFS = "autodrive_registration_draft"
        const val KEY_ACCOUNT_TYPE = "account_type"
        const val KEY_FULL_NAME = "full_name"
        const val KEY_PHONE = "phone"
        const val KEY_BANK_NAME = "bank_name"
        const val KEY_BANK_ACCOUNT = "bank_account"
        const val KEY_WORKSHOP_NAME = "workshop_name"
        const val KEY_SPECIALTY = "specialty"
        const val KEY_WORKERS_COUNT = "workers_count"
        const val KEY_ADDRESS = "address"
    }
}
