package com.autodrive.app.core.sync.data

import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.observability.AppLogger
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceReporter @Inject constructor(
    private val supabase: AutoDriveSupabase,
) {
    @Volatile
    private var lastSeenTouchedAt = 0L

    suspend fun touch(force: Boolean = false) {
        if (force) lastSeenTouchedAt = 0L
        val now = System.currentTimeMillis()
        if (now - lastSeenTouchedAt < LAST_SEEN_THROTTLE_MS) return

        runCatching { supabase.client.auth.awaitInitialization() }
        if (supabase.client.auth.currentSessionOrNull() == null) return

        runCatching { supabase.client.postgrest.rpc("touch_last_seen") }
            .onSuccess { lastSeenTouchedAt = now }
            .onFailure { error ->
                AppLogger.w(TAG, "touch_last_seen failed: ${error.message}")
            }
    }

    private companion object {
        const val TAG = "PresenceReporter"
        const val LAST_SEEN_THROTTLE_MS = 3 * 60 * 1000L
    }
}
