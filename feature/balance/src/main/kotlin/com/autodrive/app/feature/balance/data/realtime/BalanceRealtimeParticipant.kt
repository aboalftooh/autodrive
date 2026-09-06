package com.autodrive.app.feature.balance.data.realtime

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

/** Balance Realtime is non-authoritative; inserts, updates and deletes only request canonical sync. */
class BalanceRealtimeParticipant @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val hints: RealtimeHintDispatcher,
) : RealtimeParticipant {

    override val key: String = "balance"

    override suspend fun run(
        session: RealtimeSession,
        onSubscribed: () -> Unit,
    ) = coroutineScope {
        val channel = supabase.client.realtime.channel("autodrive-balance-${session.userId}")
        try {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "marketer_balance"
                filter("client_id", FilterOperator.EQ, session.clientId)
            }.onEach { action -> hintOnDataChange(action) }.launchIn(this)

            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "balance_transactions"
                filter("client_id", FilterOperator.EQ, session.clientId)
            }.onEach { action -> hintOnDataChange(action) }.launchIn(this)

            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "withdrawal_requests"
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
