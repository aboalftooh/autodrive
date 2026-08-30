package com.autodrive.app.core.sync.domain

import kotlinx.coroutines.flow.StateFlow

enum class RealtimeConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/** Truthful aggregate health; DEGRADED is distinct without forcing UI redesign in v70. */
enum class RealtimeAggregateHealth {
    DISCONNECTED,
    CONNECTING,
    DEGRADED,
    CONNECTED,
}

interface RealtimeConnectionObserver {
    val connectionState: StateFlow<RealtimeConnectionState>
    val aggregateHealth: StateFlow<RealtimeAggregateHealth>
}
