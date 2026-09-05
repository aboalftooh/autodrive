package com.autodrive.app.feature.auth.data.registration

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallationIdProvider @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "autodrive_installation",
        Context.MODE_PRIVATE,
    )

    fun get(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }?.let { return it }
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, created).commit()
        return created
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
    }
}
