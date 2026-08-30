package com.autodrive.app.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "autodrive-migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFrom4To19_preservesDataAndAddsScopedObservability() {
        createVersion4Database()

        val roomDatabase = Room.databaseBuilder(
            context,
            AutoDriveDatabase::class.java,
            databaseName,
        )
            .addMigrations(*AutoDriveDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            val database = roomDatabase.openHelper.writableDatabase

            assertEquals(AUTODRIVE_DATABASE_VERSION, database.version)
            assertEquals(
                "REQUEST_WITHDRAWAL_RPC",
                database.singleString(
                    "SELECT operation FROM pending_operations WHERE id = ?",
                    arrayOf("pending-1"),
                ),
            )
            assertEquals(
                "PENDING",
                database.singleString(
                    "SELECT status FROM pending_operations WHERE id = ?",
                    arrayOf("pending-1"),
                ),
            )
            assertEquals(
                0L,
                database.singleLong(
                    "SELECT attempt_count FROM pending_operations WHERE id = ?",
                    arrayOf("pending-1"),
                ),
            )
            assertEquals(
                0L,
                database.singleLong(
                    "SELECT next_retry_at FROM pending_operations WHERE id = ?",
                    arrayOf("pending-1"),
                ),
            )
            assertEquals(
                "pending-1",
                database.singleString(
                    "SELECT mutation_id FROM pending_operations WHERE id = ?",
                    arrayOf("pending-1"),
                ),
            )
            assertEquals("user-1", database.singleString("SELECT user_id FROM pending_operations WHERE id = ?", arrayOf("pending-1")))
            assertEquals("client-1", database.singleString("SELECT client_id FROM pending_operations WHERE id = ?", arrayOf("pending-1")))
            assertEquals("org-1", database.singleString("SELECT org_id FROM pending_operations WHERE id = ?", arrayOf("pending-1")))
            assertEquals("withdrawal_requests", database.singleString("SELECT entity_type FROM pending_operations WHERE id = ?", arrayOf("pending-1")))
            assertEquals("pending-1", database.singleString("SELECT entity_id FROM pending_operations WHERE id = ?", arrayOf("pending-1")))
            assertEquals(1L, database.singleLong("SELECT contract_version FROM pending_operations WHERE id = ?", arrayOf("pending-1")))
            assertEquals(0L, database.singleLong("SELECT lease_until FROM pending_operations WHERE id = ?", arrayOf("pending-1")))
            assertEquals(
                "PENDING",
                database.singleString(
                    "SELECT status FROM chat_messages WHERE id = ?",
                    arrayOf("message-1"),
                ),
            )
            assertEquals(
                "",
                database.singleString(
                    "SELECT subject FROM conversations WHERE id = ?",
                    arrayOf("conversation-1"),
                ),
            )
            assertNull(
                database.singleNullableString(
                    "SELECT nav_route FROM notifications WHERE id = ?",
                    arrayOf("notification-1"),
                ),
            )
            assertEquals("0.1", database.singleString("SELECT commission FROM invoices WHERE id = ?", arrayOf("invoice-1")))
            assertEquals("0.2", database.singleString("SELECT amount FROM payments WHERE id = ?", arrayOf("payment-1")))
            assertEquals("123456789.12", database.singleString("SELECT balance FROM marketer_balance WHERE id = ?", arrayOf("balance-1")))
            assertEquals("text", database.singleString("SELECT typeof(amount) FROM withdrawal_requests WHERE id = ?", arrayOf("withdrawal-1")))
            assertEquals("TEXT", database.columnType("weekly_leaderboard_cache", "total_amount"))
            assertTrue(database.hasTable("dynamo_content"))
            assertTrue(database.hasTable("weekly_leaderboard_cache"))
            assertTrue(database.hasTable("sync_cursors"))
            assertTrue(database.hasTable("sync_inbox"))
            assertTrue(database.hasTable("chat_recovery_checkpoints"))
            assertTrue(database.hasTable("chat_media_transfers"))
            assertTrue(database.hasTable("sync_bootstrap_state"))
            assertTrue(database.hasTable("sync_bootstrap_staging"))
            assertTrue(database.hasTable("sync_reconciliation_state"))
            assertTrue(database.hasTable("sync_observability_state"))
            assertEquals(0L, database.singleLong("SELECT COUNT(*) FROM sync_observability_state", emptyArray()))
            assertEquals("INTEGER", database.columnType("sync_observability_state", "bootstrap_count"))
            assertEquals("INTEGER", database.columnType("sync_observability_state", "hint_dropped_count"))
            assertEquals("client-1", database.singleString("SELECT client_id FROM payments WHERE id = ?", arrayOf("payment-1")))
            assertEquals("INTEGER", database.columnType("chat_recovery_checkpoints", "last_server_sequence"))
            assertEquals("TEXT", database.columnType("chat_messages", "media_object_path"))
            assertEquals("TEXT", database.columnType("pending_operations", "depends_on_mutation_id"))
            assertNull(database.singleNullableString("SELECT depends_on_mutation_id FROM pending_operations WHERE id = ?", arrayOf("pending-1")))
            assertEquals("TEXT", database.columnType("sync_inbox", "event_id"))
            assertEquals("TEXT", database.columnType("sync_inbox", "revision_kind"))

            val expectedIndexes = setOf(
                "index_invoices_client_id_category",
                "index_payments_invoice_id",
                "index_payments_client_id",
                "index_commission_payments_client_id",
                "index_balance_transactions_user_id_created_at",
                "index_balance_transactions_sync_status",
                "index_withdrawal_requests_user_id_created_at",
                "index_withdrawal_requests_sync_status",
                "index_withdrawal_requests_user_id_status",
                "index_notifications_user_id_created_at",
                "index_notifications_user_id_is_read_read_synced",
                "index_autodrive_users_user_id",
                "index_conversations_marketer_id_last_message_at",
                "index_conversations_marketer_id_created_at",
                "index_conversations_client_id",
                "index_chat_messages_conversation_id_created_at",
                "index_chat_messages_status_created_at",
                "index_chat_messages_sender_type_status_type",
                "index_dynamo_content_is_active",
                "index_weekly_leaderboard_cache_rank",
                "index_pending_operations_scope_status_retry_created",
                "index_pending_operations_scope_entity_status",
                "index_pending_operations_scope_mutation",
                "index_pending_operations_scope_dependency",
                "index_chat_media_transfers_scope_status_retry_created",
                "index_chat_media_transfers_scope_message",
                "index_sync_bootstrap_state_scope_bootstrap",
                "index_sync_bootstrap_staging_scope_bootstrap_entity",
            )
            assertTrue(database.indexNames().containsAll(expectedIndexes))
            assertTrue(
                database.queryPlan(
                    "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50",
                    arrayOf("user-1"),
                ).contains("index_notifications_user_id_created_at"),
            )
            assertTrue(
                database.queryPlan(
                    "SELECT * FROM chat_messages WHERE conversation_id = ? ORDER BY created_at ASC",
                    arrayOf("conversation-1"),
                ).contains("index_chat_messages_conversation_id_created_at"),
            )
            assertTrue(
                database.queryPlan(
                    "SELECT * FROM pending_operations WHERE user_id = ? AND client_id = ? AND org_id = ? AND status = 'PENDING' AND next_retry_at <= ? ORDER BY created_at ASC LIMIT 20",
                    arrayOf("user-1", "client-1", "org-1", 1000L),
                ).contains("index_pending_operations_scope_status_retry_created"),
            )
        } finally {
            roomDatabase.close()
        }
    }

    @Test
    fun unsupportedVersion_failsWithoutDeletingExistingData() {
        val path = context.getDatabasePath(databaseName)
        path.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(path, null).use { database ->
            database.execSQL("CREATE TABLE sentinel (id TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)")
            database.execSQL("INSERT INTO sentinel (id, value) VALUES ('row-1', 'keep-me')")
            database.version = 3
        }

        val roomDatabase = Room.databaseBuilder(
            context,
            AutoDriveDatabase::class.java,
            databaseName,
        )
            .addMigrations(*AutoDriveDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        val failure = runCatching {
            roomDatabase.openHelper.writableDatabase
        }.exceptionOrNull()
        roomDatabase.close()

        assertNotNull("يجب رفض النسخة غير المدعومة بدل حذفها", failure)

        SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
            database.rawQuery(
                "SELECT value FROM sentinel WHERE id = ?",
                arrayOf("row-1"),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("keep-me", cursor.getString(0))
            }
        }
    }

    private fun createVersion4Database() {
        val path = context.getDatabasePath(databaseName)
        path.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(path, null).use { database ->
            database.execSqlStatements(VERSION_4_SCHEMA)

            database.execSQL(
                """
                INSERT INTO pending_operations
                    (id, table_name, operation, payload, created_at, retry_count)
                VALUES
                    ('pending-1', 'withdrawal_requests', 'REQUEST_WITHDRAWAL_RPC', '{"amount":1000}', 100, 0)
                """.trimIndent(),
            )
            database.execSQL(
                "INSERT INTO autodrive_users (id, user_id, client_id, org_id, account_type, full_name, phone, created_at, updated_at, sync_status) " +
                    "VALUES ('profile-1', 'user-1', 'client-1', 'org-1', 'MARKETER', 'User', '000', '2026-01-01', '2026-01-01', 'SYNCED')",
            )
            database.execSQL(
                "INSERT INTO withdrawal_requests (id, user_id, client_id, amount, status, bank_name, bank_account, note, created_at, completed_at, sync_status) " +
                    "VALUES ('pending-1', 'user-1', 'client-1', 1000, 'PENDING', 'bank', 'account', NULL, '2026-01-01', NULL, 'PENDING_SYNC')",
            )
            database.execSQL(
                """
                INSERT INTO conversations
                    (id, marketer_id, client_id, title, last_message, last_message_at, unread_count, created_at)
                VALUES
                    ('conversation-1', 'marketer-1', 'client-1', 'الإدارة', 'رسالة معلقة', 100, 1, 100)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO chat_messages
                    (id, conversation_id, sender_id, sender_type, content, type, is_read, created_at, status)
                VALUES
                    ('message-1', 'conversation-1', 'marketer-1', 'MARKETER', 'صورة معلقة', 'IMAGE', 0, 100, 'PENDING')
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO notifications
                    (id, user_id, client_id, type, title, body, is_read, created_at, read_synced)
                VALUES
                    ('notification-1', 'user-1', 'client-1', 'NEW_INVOICE', 'فاتورة', 'وصلت فاتورة', 0, '2026-01-01', 1)
                """.trimIndent(),
            )
            database.execSQL("INSERT INTO invoices (id, client_id, commission, status, category, total_amount, invoice_number, created_at) VALUES ('invoice-1', 'client-1', 0.1, 'CLOSED_CASH', 'SALE', 0.3, 1, '2026-01-01')")
            database.execSQL("INSERT INTO payments (id, invoice_id, amount, created_at) VALUES ('payment-1', 'invoice-1', 0.2, '2026-01-01')")
            database.execSQL("INSERT INTO marketer_balance (id, user_id, client_id, balance, pending_withdrawal, updated_at) VALUES ('balance-1', 'user-1', 'client-1', 123456789.12, 0.1, '2026-01-01')")
            database.execSQL("INSERT INTO withdrawal_requests (id, user_id, client_id, amount, status, bank_name, bank_account, note, created_at, completed_at, sync_status) VALUES ('withdrawal-1', 'user-1', 'client-1', 12.34, 'PENDING', 'bank', 'account', NULL, '2026-01-01', NULL, 'SYNCED')")
            database.version = 4
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.singleString(
        sql: String,
        args: Array<Any>,
    ): String = query(sql, args).use { cursor ->
        assertTrue(cursor.moveToFirst())
        cursor.getString(0)
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.singleLong(
        sql: String,
        args: Array<Any>,
    ): Long = query(sql, args).use { cursor ->
        assertTrue(cursor.moveToFirst())
        cursor.getLong(0)
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.singleNullableString(
        sql: String,
        args: Array<Any>,
    ): String? = query(sql, args).use { cursor ->
        assertTrue(cursor.moveToFirst())
        if (cursor.isNull(0)) null else cursor.getString(0)
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.columnType(tableName: String, columnName: String): String =
        query("PRAGMA table_info($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) return@use cursor.getString(typeIndex)
            }
            error("Column $tableName.$columnName not found")
        }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.hasTable(tableName: String): Boolean =
        query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName),
        ).use { cursor -> cursor.moveToFirst() }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.indexNames(): Set<String> =
        query("SELECT name FROM sqlite_master WHERE type = 'index'").use { cursor ->
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.queryPlan(
        sql: String,
        args: Array<Any>,
    ): String = query("EXPLAIN QUERY PLAN $sql", args).use { cursor ->
        buildString {
            while (cursor.moveToNext()) {
                if (isNotEmpty()) append(" | ")
                append(cursor.getString(3))
            }
        }
    }

    private fun SQLiteDatabase.execSqlStatements(statements: List<String>) {
        statements.forEach(::execSQL)
    }

    private companion object {
        val VERSION_4_SCHEMA = listOf(
            """CREATE TABLE invoices (id TEXT NOT NULL, client_id TEXT NOT NULL, commission REAL NOT NULL, status TEXT NOT NULL, category TEXT NOT NULL, total_amount REAL NOT NULL, invoice_number INTEGER NOT NULL, created_at TEXT NOT NULL, PRIMARY KEY(id))""",
            """CREATE TABLE payments (id TEXT NOT NULL, invoice_id TEXT NOT NULL, amount REAL NOT NULL, created_at TEXT NOT NULL, PRIMARY KEY(id))""",
            """CREATE TABLE commission_payments (id TEXT NOT NULL, client_id TEXT NOT NULL, amount REAL NOT NULL, note TEXT, invoice_ids TEXT NOT NULL, created_at TEXT NOT NULL, PRIMARY KEY(id))""",
            """CREATE TABLE marketer_balance (id TEXT NOT NULL, user_id TEXT NOT NULL, client_id TEXT NOT NULL, balance REAL NOT NULL, pending_withdrawal REAL NOT NULL, updated_at TEXT NOT NULL, PRIMARY KEY(id))""",
            """CREATE UNIQUE INDEX index_marketer_balance_user_id ON marketer_balance (user_id)""",
            """CREATE TABLE balance_transactions (id TEXT NOT NULL, user_id TEXT NOT NULL, client_id TEXT NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL, description TEXT NOT NULL, created_at TEXT NOT NULL, sync_status TEXT NOT NULL, PRIMARY KEY(id))""",
            """CREATE TABLE withdrawal_requests (id TEXT NOT NULL, user_id TEXT NOT NULL, client_id TEXT NOT NULL, amount REAL NOT NULL, status TEXT NOT NULL, bank_name TEXT NOT NULL, bank_account TEXT NOT NULL, transaction_ref TEXT, note TEXT, created_at TEXT NOT NULL, completed_at TEXT, sync_status TEXT NOT NULL, PRIMARY KEY(id))""",
            """CREATE TABLE notifications (id TEXT NOT NULL, user_id TEXT NOT NULL, client_id TEXT NOT NULL, type TEXT NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, is_read INTEGER NOT NULL, created_at TEXT NOT NULL, read_synced INTEGER NOT NULL, PRIMARY KEY(id))""",
            """CREATE TABLE autodrive_users (id TEXT NOT NULL, user_id TEXT NOT NULL, client_id TEXT NOT NULL, org_id TEXT NOT NULL, account_type TEXT NOT NULL, full_name TEXT NOT NULL, phone TEXT NOT NULL, bank_name TEXT, bank_account TEXT, workshop_name TEXT, specialty TEXT, workers_count INTEGER, address TEXT, created_at TEXT NOT NULL, updated_at TEXT NOT NULL, sync_status TEXT NOT NULL, PRIMARY KEY(id))""",
            """CREATE TABLE conversations (id TEXT NOT NULL, marketer_id TEXT NOT NULL, client_id TEXT NOT NULL, title TEXT NOT NULL, last_message TEXT NOT NULL, last_message_at INTEGER NOT NULL, unread_count INTEGER NOT NULL, created_at INTEGER NOT NULL, PRIMARY KEY(id))""",
            """CREATE TABLE chat_messages (id TEXT NOT NULL, conversation_id TEXT NOT NULL, sender_id TEXT NOT NULL, sender_type TEXT NOT NULL, content TEXT NOT NULL, type TEXT NOT NULL, is_read INTEGER NOT NULL, created_at INTEGER NOT NULL, status TEXT NOT NULL, PRIMARY KEY(id))""",
            """CREATE TABLE pending_operations (id TEXT NOT NULL, table_name TEXT NOT NULL, operation TEXT NOT NULL, payload TEXT NOT NULL, created_at INTEGER NOT NULL, retry_count INTEGER NOT NULL, PRIMARY KEY(id))""",
        )
    }
}
