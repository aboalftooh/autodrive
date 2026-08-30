package com.autodrive.app.core.session.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.autodrive.app.core.session.domain.DashboardPreferences
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.core.session.domain.CurrentSession
import com.autodrive.app.core.session.domain.RegistrationState
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.session.domain.SessionWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SessionReader, SessionWriter, DashboardPreferences {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "autodrive_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ═══════════════════════════════════════════
    // حالة التوثيق
    // ═══════════════════════════════════════════
    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(v) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, v).apply()

    var isRegistrationComplete: Boolean
        get() = prefs.getBoolean(KEY_REGISTRATION_DONE, false)
        set(v) = prefs.edit().putBoolean(KEY_REGISTRATION_DONE, v).apply()

    // ═══════════════════════════════════════════
    // بيانات المستخدم المحلية (سريعة الوصول)
    // ═══════════════════════════════════════════
    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(v) = prefs.edit().putString(KEY_USER_ID, v).apply()

    var clientId: String?
        get() = prefs.getString(KEY_CLIENT_ID, null)
        set(v) = prefs.edit().putString(KEY_CLIENT_ID, v).apply()

    var orgId: String?
        get() = prefs.getString(KEY_ORG_ID, null)
        set(v) = prefs.edit().putString(KEY_ORG_ID, v).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(v) = prefs.edit().putString(KEY_USER_NAME, v).apply()

    var accountType: String?
        get() = prefs.getString(KEY_ACCOUNT_TYPE, null)
        set(v) = prefs.edit().putString(KEY_ACCOUNT_TYPE, v).apply()

    var phone: String?
        get() = prefs.getString(KEY_PHONE, null)
        set(v) = prefs.edit().putString(KEY_PHONE, v).apply()

    // ═══════════════════════════════════════════
    // إعدادات العمولات
    // ═══════════════════════════════════════════
    override var weeklyTarget: Money
        get() = readMoney(KEY_WEEKLY_TARGET, Money.of(500_000L))
        set(v) = prefs.edit().putString(KEY_WEEKLY_TARGET, v.toPlainString()).apply()

    override var lastDisplayedTotal: Money
        get() = readMoney(KEY_LAST_DISPLAYED_TOTAL, Money.ZERO)
        set(v) = prefs.edit().putString(KEY_LAST_DISPLAYED_TOTAL, v.toPlainString()).apply()

    override var lastDisplayedWeekStartMs: Long
        get() = prefs.getLong(KEY_LAST_DISPLAYED_WEEK_START_MS, 0L)
        set(v) = prefs.edit().putLong(KEY_LAST_DISPLAYED_WEEK_START_MS, v).apply()

    // كود الدعوة المعلّق: يُحفظ بعد verify_invite_code_v2 ويُمسح بعد redeem_invite_code
    var pendingInviteCode: String?
        get() = prefs.getString(KEY_PENDING_INVITE_CODE, null)
        set(v) = if (v != null) prefs.edit().putString(KEY_PENDING_INVITE_CODE, v).apply()
                 else prefs.edit().remove(KEY_PENDING_INVITE_CODE).apply()

    // ═══════════════════════════════════════════
    // مسح عند تسجيل الخروج
    // ═══════════════════════════════════════════
    override fun currentSession(): CurrentSession = CurrentSession(
        isLoggedIn = isLoggedIn,
        registrationState = if (isRegistrationComplete) RegistrationState.COMPLETE else RegistrationState.INCOMPLETE,
        userId = userId,
        clientId = clientId,
        orgId = orgId,
        userName = userName,
        accountType = accountType,
        phone = phone,
        pendingInviteCode = pendingInviteCode
    )

    override fun updateSession(transform: (CurrentSession) -> CurrentSession) {
        val updated = transform(currentSession())
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, updated.isLoggedIn)
            .putBoolean(KEY_REGISTRATION_DONE, updated.registrationState == RegistrationState.COMPLETE)
            .putNullableString(KEY_USER_ID, updated.userId)
            .putNullableString(KEY_CLIENT_ID, updated.clientId)
            .putNullableString(KEY_ORG_ID, updated.orgId)
            .putNullableString(KEY_USER_NAME, updated.userName)
            .putNullableString(KEY_ACCOUNT_TYPE, updated.accountType)
            .putNullableString(KEY_PHONE, updated.phone)
            .putNullableString(KEY_PENDING_INVITE_CODE, updated.pendingInviteCode)
            .apply()
    }

    override fun clearSession() {
        prefs.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_REGISTRATION_DONE)
            .remove(KEY_USER_ID)
            .remove(KEY_CLIENT_ID)
            .remove(KEY_ORG_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_ACCOUNT_TYPE)
            .remove(KEY_PHONE)
            .remove(KEY_PENDING_INVITE_CODE)
            .remove(KEY_WEEKLY_TARGET)
            .remove(KEY_LAST_DISPLAYED_TOTAL)
            .remove(KEY_LAST_DISPLAYED_WEEK_START_MS)
            .apply()
    }


    /** يقرأ صيغة String الجديدة ويقبل صيغة Double bits القديمة دون فقد التوافق. */
    private fun readMoney(key: String, default: Money): Money = when (val raw = prefs.all[key]) {
        is String -> runCatching { Money.of(raw) }.getOrDefault(default)
        is Long -> Money.fromLegacyDouble(Double.fromBits(raw))
        is Float -> Money.fromLegacyDouble(raw.toDouble())
        is Int -> Money.of(raw.toLong())
        else -> default
    }

    private fun android.content.SharedPreferences.Editor.putNullableString(
        key: String,
        value: String?
    ): android.content.SharedPreferences.Editor =
        if (value == null) remove(key) else putString(key, value)

    companion object {
        private const val KEY_IS_LOGGED_IN         = "is_logged_in"
        private const val KEY_REGISTRATION_DONE    = "registration_done"
        private const val KEY_USER_ID              = "user_id"
        private const val KEY_CLIENT_ID            = "client_id"
        private const val KEY_ORG_ID               = "org_id"
        private const val KEY_USER_NAME            = "user_name"
        private const val KEY_ACCOUNT_TYPE         = "account_type"
        private const val KEY_PHONE                = "phone"
        private const val KEY_WEEKLY_TARGET           = "weekly_target"
        private const val KEY_LAST_DISPLAYED_TOTAL    = "last_displayed_total"
        private const val KEY_LAST_DISPLAYED_WEEK_START_MS = "last_displayed_week_start_ms"
        private const val KEY_PENDING_INVITE_CODE     = "pending_invite_code"
    }
}
