package com.autodrive.app.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomPerformanceArchitectureTest {
    private val moduleDir: File = sequenceOf(
        File(System.getProperty("user.dir")),
        File(System.getProperty("user.dir"), "app"),
    ).first { File(it, "src/main").isDirectory }

    private val localRoot = ProjectLayout.source("core/database")
    private val entities = localRoot.resolve("entities/Entities.kt").readText()
    private val database = localRoot.resolve("AutoDriveDatabase.kt").readText()
    private val daos = localRoot.resolve("dao").walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .joinToString("\n") { it.readText() }

    @Test
    fun `Room version 15 registers cursor and outbox migrations`() {
        assertTrue(database.contains("AUTODRIVE_DATABASE_VERSION = 17"))
        assertTrue(database.contains("val MIGRATION_12_13"))
        assertTrue(database.contains("MIGRATION_12_13,"))
        assertTrue(database.contains("val MIGRATION_13_14"))
        assertTrue(database.contains("MIGRATION_13_14,"))
        assertTrue(database.contains("val MIGRATION_14_15"))
        assertTrue(database.contains("MIGRATION_14_15,"))
    }

    @Test
    fun `owner filters and list ordering have matching entity indexes`() {
        val expected = listOf(
            "index_invoices_client_id_category",
            "index_payments_invoice_id",
            "index_commission_payments_client_id",
            "index_balance_transactions_user_id_created_at",
            "index_withdrawal_requests_user_id_created_at",
            "index_notifications_user_id_created_at",
            "index_autodrive_users_user_id",
            "index_conversations_marketer_id_last_message_at",
            "index_conversations_marketer_id_created_at",
            "index_conversations_client_id",
            "index_chat_messages_conversation_id_created_at",
            "index_weekly_leaderboard_cache_rank",
        )
        expected.forEach { name -> assertTrue("Missing entity index: $name", entities.contains(name)) }
    }

    @Test
    fun `pending and status scans have matching indexes`() {
        val expected = listOf(
            "index_balance_transactions_sync_status",
            "index_withdrawal_requests_sync_status",
            "index_withdrawal_requests_user_id_status",
            "index_notifications_user_id_is_read_read_synced",
            "index_chat_messages_status_created_at",
            "index_chat_messages_sender_type_status_type",
            "index_dynamo_content_is_active",
            "index_pending_operations_scope_status_retry_created",
            "index_pending_operations_scope_entity_status",
            "index_pending_operations_scope_mutation",
        )
        expected.forEach { name -> assertTrue("Missing status index: $name", entities.contains(name)) }
    }

    @Test
    fun `migration creates every declared v13 index`() {
        val declaredNames = Regex("name = \\\"(index_[^\\\"]+)\\\"")
            .findAll(entities)
            .map { it.groupValues[1] }
            .filterNot { it == "index_marketer_balance_user_id" }
            .toSet()
        val migrationNames = Regex("CREATE (?:UNIQUE )?INDEX IF NOT EXISTS (index_[^ ]+)")
            .findAll(database)
            .map { it.groupValues[1] }
            .toSet()

        assertTrue(
            "Entity indexes absent from migration: ${declaredNames - migrationNames}",
            migrationNames.containsAll(declaredNames),
        )
    }

    @Test
    fun `large user-facing lists stay explicitly bounded where product behavior defines a window`() {
        assertTrue(daos.contains("ORDER BY created_at DESC LIMIT 50"))
        assertTrue(daos.contains("ORDER BY created_at DESC LIMIT 20"))
        assertTrue(daos.contains("ORDER BY created_at ASC\n        LIMIT :limit"))
        assertTrue(daos.contains("ORDER BY created_at DESC LIMIT :limit"))
    }

    @Test
    fun `foreign keys are not forced across independently synchronized aggregates`() {
        assertFalse(entities.contains("ForeignKey("))
        val decision = moduleDir.resolve("../docs/refactor/room-performance-v10.md").canonicalFile
        assertTrue("Missing relation decision", decision.isFile)
        assertTrue(decision.readText().contains("Realtime"))
        assertTrue(decision.readText().contains("Foreign Key"))
    }

    @Test
    fun `query plan verification remains part of the repository`() {
        val verifier = moduleDir.resolve("../tools/verify_room_v10.py").canonicalFile
        assertTrue(verifier.isFile)
        assertTrue(verifier.readText().contains("EXPLAIN QUERY PLAN"))
        assertTrue(verifier.readText().contains("EXPECTED_QUERY_PLANS"))
    }
}
