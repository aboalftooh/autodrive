package com.autodrive.app.core.sync.realtime

data class RealtimeSession(
    val userId: String,
    val clientId: String,
)

interface RealtimeParticipant {
    val key: String

    /** Required streams participate in global CONNECTED truth. Optional streams must opt out explicitly. */
    val required: Boolean get() = true

    suspend fun run(
        session: RealtimeSession,
        onSubscribed: () -> Unit,
    )
}
