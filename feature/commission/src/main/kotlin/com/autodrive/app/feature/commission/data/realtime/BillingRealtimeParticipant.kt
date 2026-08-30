package com.autodrive.app.feature.commission.data.realtime

import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.sync.realtime.RealtimeHintDispatcher
import com.autodrive.app.core.sync.realtime.RealtimeParticipant
import com.autodrive.app.core.sync.realtime.RealtimeSession
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import javax.inject.Inject
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Billing Realtime never applies financial state. Every data event is only a sync hint. */
class BillingRealtimeParticipant @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val hints: RealtimeHintDispatcher,
) : RealtimeParticipant {

    override val key: String = "billing"

    override suspend fun run(
        session: RealtimeSession,
        onSubscribed: () -> Unit,
    ) = coroutineScope {
        val channel = supabase.client.realtime.channel("autodrive-billing-${session.userId}")
        try {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "invoices"
                filter("client_id", FilterOperator.EQ, session.clientId)
            }.onEach { action -> hintOnDataChange(action) }.launchIn(this)

            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "commission_payments"
                filter("client_id", FilterOperator.EQ, session.clientId)
            }.onEach { action -> hintOnDataChange(action) }.launchIn(this)

            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "payments"
                filter("client_id", FilterOperator.EQ, session.clientId)
            }.onEach { action -> hintOnDataChange(action) }.launchIn(this)

            channel.subscribe()
            onSubscribed()
            awaitCancellation()
        } finally {
            runCatching { supabase.client.realtime.removeChannel(channel) }
        }
    }

    private fun hintOnDataChange(action: PostgresAction) {
        when (action) {
            is PostgresAction.Insert,
            is PostgresAction.Update,
            is PostgresAction.Delete -> hints.requestSync()
            else -> Unit
        }
    }
}
