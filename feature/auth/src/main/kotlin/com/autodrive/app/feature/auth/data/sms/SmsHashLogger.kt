package com.autodrive.app.feature.auth.data.sms

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import com.autodrive.app.feature.auth.BuildConfig
import java.security.MessageDigest

/**
 * DEBUG-ONLY utility — يُحسب hash التطبيق الحقيقي المطلوب في رسائل SMS Retriever.
 * لا يُنفَّذ أبداً في release builds.
 * احذف هذا الملف بعد التحقق من الـ hash.
 */
object SmsHashLogger {

    private const val TAG = "SMS_HASH_VERIFY"

    fun log(context: Context) {
        if (!BuildConfig.DEBUG) return

        val packageName = context.packageName
        Log.d(TAG, "=== SMS Retriever Hash Verification ===")
        Log.d(TAG, "Package: $packageName")
        Log.d(TAG, "Build type: DEBUG")

        val hashes = computeHashes(context)
        if (hashes.isEmpty()) {
            Log.e(TAG, "ERROR: Failed to compute app signatures")
            return
        }

        Log.d(TAG, "--- Computed Hashes (add one of these to SMS message) ---")
        hashes.forEachIndexed { i, hash ->
            Log.d(TAG, "Hash[$i]: $hash")
        }
        Log.d(TAG, "Expected SMS format:")
        Log.d(TAG, "<#> Your AutoDrive code: XXXXXX\n${hashes.firstOrNull() ?: "ERROR"}")
        Log.d(TAG, "========================================")
    }

    @Suppress("DEPRECATION")
    private fun computeHashes(context: Context): List<String> {
        return try {
            val pm = context.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo
                    ?.apkContentsSigners
                    ?: emptyArray()
            } else {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                    .signatures
                    ?: emptyArray()
            }

            signatures.mapNotNull { sig ->
                val appInfo = "${context.packageName} ${sig.toCharsString()}"
                val digest = MessageDigest.getInstance("SHA-256")
                    .digest(appInfo.toByteArray(Charsets.UTF_8))
                val base64 = Base64.encodeToString(digest, Base64.NO_WRAP or Base64.NO_PADDING)
                base64.take(11)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error computing hash: ${e.message}")
            emptyList()
        }
    }
}
