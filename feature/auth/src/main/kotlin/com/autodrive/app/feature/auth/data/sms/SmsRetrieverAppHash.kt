package com.autodrive.app.feature.auth.data.sms

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import java.security.MessageDigest

/** Computes the 11-character SMS Retriever app hash for the currently installed APK signature. */
object SmsRetrieverAppHash {

    @Suppress("DEPRECATION")
    fun current(context: Context): String? = runCatching {
        val appContext = context.applicationContext
        val packageName = appContext.packageName
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            appContext.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
        } else {
            appContext.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES,
            )
        }

        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners.orEmpty()
        } else {
            packageInfo.signatures.orEmpty()
        }

        signatures.firstNotNullOfOrNull { signature ->
            val source = "$packageName ${signature.toCharsString()}"
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(source.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(
                digest,
                Base64.NO_WRAP or Base64.NO_PADDING,
            ).take(11).takeIf { it.length == 11 }
        }
    }.getOrNull()
}
