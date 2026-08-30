package com.autodrive.app.core.sync.realtime

import com.autodrive.app.core.observability.AppLogger
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.diagnostics.SyncDiagnostics
import com.autodrive.app.core.sync.diagnostics.SyncObservabilityStore
import com.autodrive.app.core.sync.data.SyncScope
import com.autodrive.app.core.sync.domain.RealtimeAggregateHealth
import com.autodrive.app.core.sync.domain.RealtimeConnectionObserver
import com.autodrive.app.core.sync.domain.RealtimeConnectionState
import com.autodrive.app.core.sync.domain.RealtimeController
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@Singleton
class RealtimeManager @Inject constructor(
    private val sessionReader: SessionReader,
    participants: Set<@JvmSuppressWildcards RealtimeParticipant>,
    private val diagnostics: SyncDiagnostics,
    private val observabilityStore: SyncObservabilityStore,
) : RealtimeController, RealtimeConnectionObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val participants: List<RealtimeParticipant> = participants.sortedBy { it.key }
    private val jobLock = Any()
    private val healthLock = Any()
    private var activeJob: Job? = null
    private val participantHealth = mutableMapOf<String, ParticipantHealth>()

    private val _connectionState = MutableStateFlow(RealtimeConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<RealtimeConnectionState> = _connectionState.asStateFlow()

    private val _aggregateHealth = MutableStateFlow(RealtimeAggregateHealth.DISCONNECTED)
    override val aggregateHealth: StateFlow<RealtimeAggregateHealth> = _aggregateHealth.asStateFlow()

    override fun restart() {
        val next = synchronized(jobLock) {
            val previous = activeJob
            scope.launch(start = CoroutineStart.LAZY) {
                previous?.cancelAndJoin()
                connectCurrentSession()
            }.also { activeJob = it }
        }
        next.start()
    }

    override fun stop() {
        synchronized(jobLock) {
            activeJob?.cancel()
            activeJob = null
        }
        resetHealth()
    }

    private suspend fun connectCurrentSession() {
        val current = sessionReader.currentSession()
        val exactScope = SyncScope.from(current)
        if (exactScope == null) {
            resetHealth()
            return
        }
        val session = RealtimeSession(userId = exactScope.userId, clientId = exactScope.clientId)
        try {
            connectParticipants(session, exactScope)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } finally {
            resetHealth(exactScope)
        }
    }

    private suspend fun connectParticipants(session: RealtimeSession, exactScope: SyncScope): Unit = supervisorScope {
        if (participants.isEmpty()) {
            resetHealth()
            return@supervisorScope
        }

        synchronized(healthLock) {
            participantHealth.clear()
            participants.forEach { participantHealth[it.key] = ParticipantHealth.CONNECTING }
        }
        publishAggregate(exactScope = exactScope)

        participants.forEach { participant ->
            launch {
                var retryDelayMs = INITIAL_RECONNECT_DELAY_MS
                while (currentCoroutineContext().isActive) {
                    updateParticipantHealth(participant.key, ParticipantHealth.CONNECTING, retryDelayMs, exactScope)
                    try {
                        participant.run(session) {
                            retryDelayMs = INITIAL_RECONNECT_DELAY_MS
                            updateParticipantHealth(participant.key, ParticipantHealth.HEALTHY, exactScope = exactScope)
                        }
                        updateParticipantHealth(participant.key, ParticipantHealth.RETRYING, retryDelayMs, exactScope)
                        AppLogger.w(TAG, "Realtime participant stopped: ${participant.key}")
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Throwable) {
                        updateParticipantHealth(participant.key, ParticipantHealth.RETRYING, retryDelayMs, exactScope)
                        AppLogger.w(
                            TAG,
                            "Realtime participant unavailable [${participant.key}]: ${error::class.simpleName}",
                        )
                    }
                    delay(retryDelayMs)
                    retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
                }
            }
        }
    }

    private fun updateParticipantHealth(
        key: String,
        health: ParticipantHealth,
        reconnectDelayMs: Long? = null,
        exactScope: SyncScope,
    ) {
        synchronized(healthLock) { participantHealth[key] = health }
        publishAggregate(reconnectDelayMs, exactScope)
    }

    private fun publishAggregate(reconnectDelayMs: Long? = null, exactScope: SyncScope? = null) {
        val (aggregate, failedRequired) = synchronized(healthLock) {
            val required = participants.filter { it.required }
            val states = required.map { participantHealth[it.key] ?: ParticipantHealth.CONNECTING }
            val aggregateState = when {
                required.isEmpty() -> RealtimeAggregateHealth.DISCONNECTED
                states.all { it == ParticipantHealth.HEALTHY } -> RealtimeAggregateHealth.CONNECTED
                states.any { it == ParticipantHealth.HEALTHY } -> RealtimeAggregateHealth.DEGRADED
                states.any { it == ParticipantHealth.CONNECTING } -> RealtimeAggregateHealth.CONNECTING
                else -> RealtimeAggregateHealth.DISCONNECTED
            }
            aggregateState to states.count { it != ParticipantHealth.HEALTHY }
        }
        _aggregateHealth.value = aggregate
        val currentScope = SyncScope.from(sessionReader.currentSession())
        val targetScope = exactScope ?: currentScope
        if (targetScope != null && currentScope == targetScope) {
            scope.launch {
                if (SyncScope.from(sessionReader.currentSession()) == targetScope) {
                    runCatching { observabilityStore.realtime(targetScope, aggregate.name, failedRequired) }
                }
            }
        }
        val compatibilityState = when (aggregate) {
            RealtimeAggregateHealth.CONNECTED -> RealtimeConnectionState.CONNECTED
            RealtimeAggregateHealth.DISCONNECTED -> RealtimeConnectionState.DISCONNECTED
            RealtimeAggregateHealth.CONNECTING,
            RealtimeAggregateHealth.DEGRADED -> RealtimeConnectionState.CONNECTING
        }
        setConnectionState(compatibilityState, reconnectDelayMs)
    }

    private fun resetHealth(exactScope: SyncScope? = null) {
        synchronized(healthLock) {
            participantHealth.clear()
            participants.forEach { participantHealth[it.key] = ParticipantHealth.DISCONNECTED }
        }
        _aggregateHealth.value = RealtimeAggregateHealth.DISCONNECTED
        val currentScope = SyncScope.from(sessionReader.currentSession())
        val targetScope = exactScope ?: currentScope
        if (targetScope != null && currentScope == targetScope) {
            val failed = participants.count { it.required }
            scope.launch {
                if (SyncScope.from(sessionReader.currentSession()) == targetScope) {
                    runCatching { observabilityStore.realtime(targetScope, RealtimeAggregateHealth.DISCONNECTED.name, failed) }
                }
            }
        }
        setConnectionState(RealtimeConnectionState.DISCONNECTED)
    }

    private fun setConnectionState(
        state: RealtimeConnectionState,
        reconnectDelayMs: Long? = null,
    ) {
        _connectionState.value = state
        diagnostics.realtimeState(state, reconnectDelayMs)
    }

    private enum class ParticipantHealth {
        CONNECTING,
        HEALTHY,
        RETRYING,
        DISCONNECTED,
    }

    private companion object {
        const val TAG = "RealtimeManager"
        const val INITIAL_RECONNECT_DELAY_MS = 1_000L
        const val MAX_RECONNECT_DELAY_MS = 60_000L
    }
}
