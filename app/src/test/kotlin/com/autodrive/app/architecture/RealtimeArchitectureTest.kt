package com.autodrive.app.architecture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeArchitectureTest {

    private val root = ProjectLayout.mergedAppRoot
    private val participantFiles = listOf(
        root.resolve("feature/commission/data/realtime/BillingRealtimeParticipant.kt"),
        root.resolve("feature/balance/data/realtime/BalanceRealtimeParticipant.kt"),
        root.resolve("feature/chat/data/realtime/ChatRealtimeParticipant.kt"),
        root.resolve("feature/notifications/data/realtime/NotificationsRealtimeParticipant.kt"),
    )

    @Test
    fun `SyncManager no longer owns realtime subscriptions`() {
        val source = root.resolve("core/sync/data/SyncManager.kt").readText()
        assertFalse(source.contains("postgresChangeFlow"))
        assertFalse(source.contains("PostgresAction"))
        assertFalse(source.contains("restartRealtime"))
    }

    @Test
    fun `all realtime data events are hint only`() {
        participantFiles.forEach { file ->
            val source = file.readText()
            assertTrue("${file.name} handles insert", source.contains("PostgresAction.Insert"))
            assertTrue("${file.name} handles update", source.contains("PostgresAction.Update"))
            assertTrue("${file.name} handles delete", source.contains("PostgresAction.Delete"))
            assertTrue("${file.name} requests authoritative sync", source.contains("hints.requestSync()"))
            assertFalse("${file.name} must not own Room", source.contains("AutoDriveDatabase"))
            assertFalse("${file.name} must not use oldRecord", source.contains("oldRecord"))
            assertFalse("${file.name} must not publish payload notification", source.contains("LocalNotificationPublisher"))
            assertFalse("${file.name} must not use targeted Room refresher", source.contains("BillingTargetedRefresher"))
        }
    }

    @Test
    fun `owner bearing channels retain server filters`() {
        val billing = participantFiles[0].readText()
        val balance = participantFiles[1].readText()
        val chat = participantFiles[2].readText()
        val notifications = participantFiles[3].readText()

        assertTrue(billing.contains("filter(\"client_id\""))
        assertTrue(balance.contains("filter(\"client_id\""))
        assertTrue(chat.contains("filter(\"client_id\""))
        assertTrue(notifications.contains("filter(\"user_id\""))
    }

    @Test
    fun `aggregate connected requires every required participant healthy`() {
        val manager = root.resolve("core/sync/realtime/RealtimeManager.kt").readText()
        val observer = root.resolve("core/sync/domain/RealtimeConnectionObserver.kt").readText()
        assertTrue(observer.contains("DEGRADED"))
        assertTrue(manager.contains("states.all { it == ParticipantHealth.HEALTHY }"))
        assertTrue(manager.contains("RealtimeAggregateHealth.DEGRADED"))
        assertFalse(manager.contains("subscribed.receive()"))
        assertFalse(manager.contains("Channel<"))
    }

    @Test
    fun `realtime manager coordinates participants without database details`() {
        val source = root.resolve("core/sync/realtime/RealtimeManager.kt").readText()
        assertTrue(source.contains("RealtimeParticipant"))
        assertFalse(source.contains("AutoDriveDatabase"))
        assertFalse(source.contains("postgresChangeFlow"))
    }
}
