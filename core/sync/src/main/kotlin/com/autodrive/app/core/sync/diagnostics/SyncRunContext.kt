package com.autodrive.app.core.sync.diagnostics

import android.content.Context
import com.autodrive.app.core.sync.data.SyncScope
import com.autodrive.app.core.sync.domain.SyncReason
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/** One logical coordinator execution. Never reused across trailing generations. */
data class SyncRunContext(
    val syncRunId: String,
    val reason: SyncReason,
    val requestedGeneration: Long,
    val startedAtLocal: Long,
    val scopeFingerprint: String,
)

@Singleton
class ScopeFingerprintProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun fingerprint(scope: SyncScope): String {
        val salt = installSalt()
        val canonical = "v1|$salt|${scope.userId}|${scope.clientId}|${scope.orgId}"
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            .take(20)
    }

    private fun installSalt(): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_SALT, null)?.let { if (it.length >= 32) return it }
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        val created = bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        prefs.edit().putString(KEY_SALT, created).apply()
        return created
    }

    private companion object {
        const val PREFS = "sync_diagnostics_v1"
        const val KEY_SALT = "install_salt"
    }
}
