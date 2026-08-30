package com.autodrive.app.core.platform.notifications

import android.content.Context

object FcmTokenStore {
    private const val PREFS = "autodrive_fcm_prefs"
    private const val KEY_LAST_UPLOADED = "last_uploaded_token"
    private const val KEY_PENDING       = "pending_token"

    fun lastUploaded(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_UPLOADED, null)

    fun setLastUploaded(context: Context, token: String?) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_UPLOADED, token).apply()
    }

    fun pending(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PENDING, null)

    fun setPending(context: Context, token: String?) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PENDING, token).apply()
    }
}
