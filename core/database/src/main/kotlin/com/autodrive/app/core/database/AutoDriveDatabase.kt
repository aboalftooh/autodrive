package com.autodrive.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.autodrive.app.core.database.dao.*
import com.autodrive.app.core.database.converters.BigDecimalConverters
import com.autodrive.app.core.database.entities.*

const val AUTODRIVE_DATABASE_VERSION = 19

@TypeConverters(BigDecimalConverters::class)
@Database(
    entities = [
        InvoiceEntity::class,
        PaymentEntity::class,
        CommissionPaymentEntity::class,
        MarketerBalanceEntity::class,
        BalanceTransactionEntity::class,
        WithdrawalRequestEntity::class,
        NotificationEntity::class,
        AutoDriveUserEntity::class,
        PendingOperationEntity::class,
        ConversationEntity::class,
        ChatMessageEntity::class,
        DynamoContentEntity::class,
        WeeklyLeaderboardEntity::class,
        SyncCursorEntity::class,
        SyncInboxEntity::class,
        ChatRecoveryCheckpointEntity::class,
        ChatMediaTransferEntity::class,
        SyncBootstrapStateEntity::class,
        SyncBootstrapStagingEntity::class,
        SyncReconciliationStateEntity::class,
        SyncObservabilityStateEntity::class,
    ],
    version = AUTODRIVE_DATABASE_VERSION,
    exportSchema = true
)
abstract class AutoDriveDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun commissionPaymentDao(): CommissionPaymentDao
    abstract fun marketerBalanceDao(): MarketerBalanceDao
    abstract fun balanceTransactionDao(): BalanceTransactionDao
    abstract fun withdrawalRequestDao(): WithdrawalRequestDao
    abstract fun notificationDao(): NotificationDao
    abstract fun autoDriveUserDao(): AutoDriveUserDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun dynamoContentDao(): DynamoContentDao
    abstract fun weeklyLeaderboardDao(): WeeklyLeaderboardDao
    abstract fun syncCursorDao(): SyncCursorDao
    abstract fun syncInboxDao(): SyncInboxDao
    abstract fun chatRecoveryCheckpointDao(): ChatRecoveryCheckpointDao
    abstract fun chatMediaTransferDao(): ChatMediaTransferDao
    abstract fun syncBootstrapDao(): SyncBootstrapDao
    abstract fun syncReconciliationStateDao(): SyncReconciliationStateDao
    abstract fun syncObservabilityDao(): SyncObservabilityDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE conversations ADD COLUMN subject TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN media_url TEXT")
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN media_mime TEXT")
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN media_duration_ms INTEGER")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS dynamo_content (
                        id TEXT NOT NULL PRIMARY KEY,
                        content_type TEXT NOT NULL,
                        audience_type TEXT NOT NULL,
                        specialty TEXT NOT NULL DEFAULT 'general',
                        message TEXT NOT NULL,
                        priority INTEGER NOT NULL DEFAULT 1,
                        is_active INTEGER NOT NULL DEFAULT 1,
                        created_at TEXT NOT NULL DEFAULT '',
                        synced_at INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN local_path TEXT")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weekly_leaderboard_cache (
                        id TEXT NOT NULL PRIMARY KEY,
                        rank INTEGER NOT NULL,
                        total_amount REAL NOT NULL,
                        is_me INTEGER NOT NULL,
                        week_number INTEGER NOT NULL,
                        synced_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // بند 9: عمود nav_route لتوجيه إشعار ADMIN_REMINDER من القائمة الداخلية
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notifications ADD COLUMN nav_route TEXT")
            }
        }


        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_operations_v11 (
                        id TEXT NOT NULL PRIMARY KEY,
                        table_name TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        attempt_count INTEGER NOT NULL,
                        next_retry_at INTEGER NOT NULL,
                        last_error_code TEXT,
                        last_error_message TEXT,
                        payload_version INTEGER NOT NULL,
                        idempotency_key TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO pending_operations_v11 (
                        id,
                        table_name,
                        operation,
                        payload,
                        created_at,
                        status,
                        attempt_count,
                        next_retry_at,
                        last_error_code,
                        last_error_message,
                        payload_version,
                        idempotency_key
                    )
                    SELECT
                        id,
                        table_name,
                        operation,
                        payload,
                        created_at,
                        'PENDING',
                        retry_count,
                        0,
                        NULL,
                        NULL,
                        1,
                        id
                    FROM pending_operations
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE pending_operations")
                database.execSQL("ALTER TABLE pending_operations_v11 RENAME TO pending_operations")
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_pending_operations_idempotency_key " +
                        "ON pending_operations (idempotency_key)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_pending_operations_status_next_retry_at " +
                        "ON pending_operations (status, next_retry_at)"
                )
            }
        }

        /**
         * يحول جميع الأعمدة المالية من SQLite REAL إلى TEXT decimal دون المرور
         * عبر Double. CAST يحفظ التمثيل العشري الظاهر للقيم القديمة، ثم تصبح
         * الكتابات الجديدة BigDecimal عبر TypeConverter.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("PRAGMA foreign_keys=OFF")

                database.execSQL("""
                    CREATE TABLE invoices_v12 (
                        id TEXT NOT NULL PRIMARY KEY,
                        client_id TEXT NOT NULL,
                        commission TEXT NOT NULL,
                        status TEXT NOT NULL,
                        category TEXT NOT NULL,
                        total_amount TEXT NOT NULL,
                        invoice_number INTEGER NOT NULL,
                        created_at TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO invoices_v12
                    SELECT id, client_id, CAST(commission AS TEXT), status, category,
                           CAST(total_amount AS TEXT), invoice_number, created_at
                    FROM invoices
                """.trimIndent())
                replaceTable(database, "invoices", "invoices_v12")

                database.execSQL("""
                    CREATE TABLE payments_v12 (
                        id TEXT NOT NULL PRIMARY KEY,
                        invoice_id TEXT NOT NULL,
                        amount TEXT NOT NULL,
                        created_at TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO payments_v12
                    SELECT id, invoice_id, CAST(amount AS TEXT), created_at FROM payments
                """.trimIndent())
                replaceTable(database, "payments", "payments_v12")

                database.execSQL("""
                    CREATE TABLE commission_payments_v12 (
                        id TEXT NOT NULL PRIMARY KEY,
                        client_id TEXT NOT NULL,
                        amount TEXT NOT NULL,
                        note TEXT,
                        invoice_ids TEXT NOT NULL,
                        created_at TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO commission_payments_v12
                    SELECT id, client_id, CAST(amount AS TEXT), note, invoice_ids, created_at
                    FROM commission_payments
                """.trimIndent())
                replaceTable(database, "commission_payments", "commission_payments_v12")

                database.execSQL("""
                    CREATE TABLE marketer_balance_v12 (
                        id TEXT NOT NULL PRIMARY KEY,
                        user_id TEXT NOT NULL,
                        client_id TEXT NOT NULL,
                        balance TEXT NOT NULL,
                        pending_withdrawal TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO marketer_balance_v12
                    SELECT id, user_id, client_id, CAST(balance AS TEXT),
                           CAST(pending_withdrawal AS TEXT), updated_at
                    FROM marketer_balance
                """.trimIndent())
                replaceTable(database, "marketer_balance", "marketer_balance_v12")
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_marketer_balance_user_id " +
                        "ON marketer_balance (user_id)"
                )

                database.execSQL("""
                    CREATE TABLE balance_transactions_v12 (
                        id TEXT NOT NULL PRIMARY KEY,
                        user_id TEXT NOT NULL,
                        client_id TEXT NOT NULL,
                        type TEXT NOT NULL,
                        amount TEXT NOT NULL,
                        description TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        sync_status TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO balance_transactions_v12
                    SELECT id, user_id, client_id, type, CAST(amount AS TEXT), description,
                           created_at, sync_status
                    FROM balance_transactions
                """.trimIndent())
                replaceTable(database, "balance_transactions", "balance_transactions_v12")

                database.execSQL("""
                    CREATE TABLE withdrawal_requests_v12 (
                        id TEXT NOT NULL PRIMARY KEY,
                        user_id TEXT NOT NULL,
                        client_id TEXT NOT NULL,
                        amount TEXT NOT NULL,
                        status TEXT NOT NULL,
                        bank_name TEXT NOT NULL,
                        bank_account TEXT NOT NULL,
                        transaction_ref TEXT,
                        note TEXT,
                        created_at TEXT NOT NULL,
                        completed_at TEXT,
                        sync_status TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO withdrawal_requests_v12
                    SELECT id, user_id, client_id, CAST(amount AS TEXT), status, bank_name,
                           bank_account, transaction_ref, note, created_at, completed_at, sync_status
                    FROM withdrawal_requests
                """.trimIndent())
                replaceTable(database, "withdrawal_requests", "withdrawal_requests_v12")

                database.execSQL("""
                    CREATE TABLE weekly_leaderboard_cache_v12 (
                        id TEXT NOT NULL PRIMARY KEY,
                        rank INTEGER NOT NULL,
                        total_amount TEXT NOT NULL,
                        is_me INTEGER NOT NULL,
                        week_number INTEGER NOT NULL,
                        synced_at INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO weekly_leaderboard_cache_v12
                    SELECT id, rank, CAST(total_amount AS TEXT), is_me, week_number, synced_at
                    FROM weekly_leaderboard_cache
                """.trimIndent())
                replaceTable(database, "weekly_leaderboard_cache", "weekly_leaderboard_cache_v12")

                database.execSQL("PRAGMA foreign_keys=ON")
            }
        }

        /**
         * يضيف فهارس مبنية على استعلامات DAO الفعلية دون فرض Foreign Keys قد
         * تكسر Offline-first عند وصول أحداث Realtime بترتيب مختلف.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                ROOM_V13_INDEXES.forEach(database::execSQL)
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_cursors (
                        user_id TEXT NOT NULL,
                        client_id TEXT NOT NULL,
                        org_id TEXT NOT NULL,
                        stream TEXT NOT NULL,
                        cursor_token TEXT NOT NULL,
                        contract_version INTEGER NOT NULL DEFAULT 1,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY(user_id, client_id, org_id, stream)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * v68: scopes every durable outbound mutation and separates retry scheduling from claim leases.
         * Legacy v14 rows are migrated only when their owner can be proven from Room data.
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val unresolved = database.query(
                    """
                    SELECT COUNT(*)
                    FROM pending_operations p
                    WHERE NOT (
                        p.operation = 'UPDATE_PROFILE'
                        AND EXISTS (
                            SELECT 1 FROM autodrive_users u
                            WHERE p.idempotency_key = 'profile:' || u.user_id
                        )
                    )
                    AND NOT (
                        p.operation = 'REQUEST_WITHDRAWAL_RPC'
                        AND EXISTS (
                            SELECT 1
                            FROM withdrawal_requests w
                            JOIN autodrive_users u
                              ON u.user_id = w.user_id
                             AND u.client_id = w.client_id
                            WHERE w.id = p.idempotency_key
                        )
                    )
                    """.trimIndent()
                ).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else 0L
                }
                if (unresolved != 0L) {
                    throw IllegalStateException("MIGRATION_UNSCOPED_OUTBOX_ROW")
                }

                database.execSQL(
                    """
                    CREATE TABLE pending_operations_v15 (
                        id TEXT NOT NULL PRIMARY KEY,
                        mutation_id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        client_id TEXT NOT NULL,
                        org_id TEXT NOT NULL,
                        entity_type TEXT NOT NULL,
                        entity_id TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        contract_version INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        attempt_count INTEGER NOT NULL,
                        next_retry_at INTEGER NOT NULL,
                        lease_until INTEGER NOT NULL,
                        last_error_code TEXT,
                        last_error_message TEXT
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    INSERT INTO pending_operations_v15 (
                        id, mutation_id, user_id, client_id, org_id,
                        entity_type, entity_id, operation, payload, contract_version,
                        created_at, status, attempt_count, next_retry_at, lease_until,
                        last_error_code, last_error_message
                    )
                    SELECT
                        p.id,
                        p.id,
                        u.user_id,
                        u.client_id,
                        u.org_id,
                        'autodrive_users',
                        u.user_id,
                        'UPDATE_PROFILE',
                        p.payload,
                        p.payload_version,
                        p.created_at,
                        p.status,
                        p.attempt_count,
                        CASE WHEN p.status = 'IN_PROGRESS' THEN 0 ELSE p.next_retry_at END,
                        CASE WHEN p.status = 'IN_PROGRESS' THEN p.next_retry_at ELSE 0 END,
                        p.last_error_code,
                        p.last_error_message
                    FROM pending_operations p
                    JOIN autodrive_users u
                      ON p.idempotency_key = 'profile:' || u.user_id
                    WHERE p.operation = 'UPDATE_PROFILE'
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    INSERT INTO pending_operations_v15 (
                        id, mutation_id, user_id, client_id, org_id,
                        entity_type, entity_id, operation, payload, contract_version,
                        created_at, status, attempt_count, next_retry_at, lease_until,
                        last_error_code, last_error_message
                    )
                    SELECT
                        p.id,
                        p.idempotency_key,
                        w.user_id,
                        w.client_id,
                        u.org_id,
                        'withdrawal_requests',
                        p.idempotency_key,
                        'REQUEST_WITHDRAWAL_RPC',
                        p.payload,
                        p.payload_version,
                        p.created_at,
                        p.status,
                        p.attempt_count,
                        CASE WHEN p.status = 'IN_PROGRESS' THEN 0 ELSE p.next_retry_at END,
                        CASE WHEN p.status = 'IN_PROGRESS' THEN p.next_retry_at ELSE 0 END,
                        p.last_error_code,
                        p.last_error_message
                    FROM pending_operations p
                    JOIN withdrawal_requests w ON w.id = p.idempotency_key
                    JOIN autodrive_users u
                      ON u.user_id = w.user_id
                     AND u.client_id = w.client_id
                    WHERE p.operation = 'REQUEST_WITHDRAWAL_RPC'
                    """.trimIndent()
                )

                val oldCount = database.query("SELECT COUNT(*) FROM pending_operations").use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else 0L
                }
                val newCount = database.query("SELECT COUNT(*) FROM pending_operations_v15").use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else 0L
                }
                if (oldCount != newCount) {
                    throw IllegalStateException("MIGRATION_OUTBOX_ROW_COUNT_MISMATCH")
                }

                database.execSQL("DROP TABLE pending_operations")
                database.execSQL("ALTER TABLE pending_operations_v15 RENAME TO pending_operations")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_pending_operations_scope_status_retry_created " +
                        "ON pending_operations (user_id, client_id, org_id, status, next_retry_at, created_at)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_pending_operations_scope_entity_status " +
                        "ON pending_operations (user_id, client_id, org_id, entity_type, entity_id, status)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_pending_operations_scope_mutation " +
                        "ON pending_operations (user_id, client_id, org_id, mutation_id)"
                )
            }
        }

        /** v70: durable scoped inbound event ledger. No payload or synthetic revision is persisted. */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_inbox (
                        user_id TEXT NOT NULL,
                        client_id TEXT NOT NULL,
                        org_id TEXT NOT NULL,
                        stream TEXT NOT NULL,
                        event_id TEXT NOT NULL,
                        server_revision TEXT,
                        revision_kind TEXT NOT NULL,
                        entity_type TEXT NOT NULL,
                        entity_id TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        transaction_group_id TEXT,
                        received_at INTEGER NOT NULL,
                        applied_at INTEGER,
                        contract_version INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(user_id, client_id, org_id, stream, event_id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sync_inbox_scope_stream_applied " +
                        "ON sync_inbox (user_id, client_id, org_id, stream, applied_at)",
                )
            }
        }

        /** v71: scoped chat recovery checkpoints + durable outbound media queue + Outbox dependency. */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE pending_operations ADD COLUMN depends_on_mutation_id TEXT")
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN media_object_path TEXT")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_pending_operations_scope_dependency " +
                        "ON pending_operations (user_id, client_id, org_id, depends_on_mutation_id)",
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_recovery_checkpoints (
                        user_id TEXT NOT NULL, client_id TEXT NOT NULL, org_id TEXT NOT NULL,
                        conversation_id TEXT NOT NULL, last_created_at_server TEXT NOT NULL,
                        last_message_id TEXT NOT NULL, last_server_sequence INTEGER NOT NULL DEFAULT 0,
                        contract_version INTEGER NOT NULL DEFAULT 2, updated_at_local INTEGER NOT NULL,
                        PRIMARY KEY(user_id, client_id, org_id, conversation_id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_media_transfers (
                        transfer_id TEXT NOT NULL PRIMARY KEY, user_id TEXT NOT NULL, client_id TEXT NOT NULL,
                        org_id TEXT NOT NULL, message_id TEXT NOT NULL, direction TEXT NOT NULL,
                        local_path TEXT NOT NULL, media_mime TEXT NOT NULL, size_bytes INTEGER NOT NULL,
                        content_sha256 TEXT NOT NULL, bucket TEXT NOT NULL, object_path TEXT NOT NULL,
                        remote_reference TEXT, status TEXT NOT NULL, attempt_count INTEGER NOT NULL,
                        next_retry_at INTEGER NOT NULL, lease_until INTEGER NOT NULL, last_error_code TEXT,
                        created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_chat_media_transfers_scope_status_retry_created " +
                        "ON chat_media_transfers (user_id, client_id, org_id, status, next_retry_at, created_at)",
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_chat_media_transfers_scope_message " +
                        "ON chat_media_transfers (user_id, client_id, org_id, message_id)",
                )
            }
        }

        /** v72: durable scoped bootstrap staging and reconciliation scheduling state. */
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE payments ADD COLUMN client_id TEXT NOT NULL DEFAULT ''")
                database.execSQL("UPDATE payments SET client_id = COALESCE((SELECT client_id FROM invoices WHERE invoices.id = payments.invoice_id LIMIT 1), '')")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_payments_client_id ON payments(client_id)")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_bootstrap_state (
                        user_id TEXT NOT NULL,
                        client_id TEXT NOT NULL,
                        org_id TEXT NOT NULL,
                        stream TEXT NOT NULL,
                        bootstrap_id TEXT NOT NULL,
                        baseline_revision TEXT NOT NULL,
                        status TEXT NOT NULL,
                        contract_version INTEGER NOT NULL,
                        next_page_token TEXT,
                        started_at_local INTEGER NOT NULL,
                        updated_at_local INTEGER NOT NULL,
                        PRIMARY KEY(user_id, client_id, org_id, stream)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_sync_bootstrap_state_scope_bootstrap " +
                        "ON sync_bootstrap_state (user_id, client_id, org_id, bootstrap_id)",
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_bootstrap_staging (
                        user_id TEXT NOT NULL,
                        client_id TEXT NOT NULL,
                        org_id TEXT NOT NULL,
                        bootstrap_id TEXT NOT NULL,
                        entity_type TEXT NOT NULL,
                        entity_id TEXT NOT NULL,
                        canonical_payload TEXT NOT NULL,
                        canonical_digest TEXT,
                        PRIMARY KEY(user_id, client_id, org_id, bootstrap_id, entity_type, entity_id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sync_bootstrap_staging_scope_bootstrap_entity " +
                        "ON sync_bootstrap_staging (user_id, client_id, org_id, bootstrap_id, entity_type)",
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_reconciliation_state (
                        user_id TEXT NOT NULL,
                        client_id TEXT NOT NULL,
                        org_id TEXT NOT NULL,
                        stream TEXT NOT NULL,
                        last_checked_revision TEXT,
                        last_result TEXT NOT NULL,
                        contract_version INTEGER NOT NULL,
                        next_due_at_local INTEGER NOT NULL,
                        updated_at_local INTEGER NOT NULL,
                        PRIMARY KEY(user_id, client_id, org_id, stream)
                    )
                    """.trimIndent(),
                )
            }
        }

        /** v73: append-only exact-scope diagnostic state; unknown history remains NULL/0. */
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_observability_state (
                        user_id TEXT NOT NULL,
                        client_id TEXT NOT NULL,
                        org_id TEXT NOT NULL,
                        stream TEXT NOT NULL,
                        contract_version INTEGER NOT NULL,
                        last_sync_run_id TEXT,
                        last_sync_status TEXT,
                        last_sync_started_at_local INTEGER,
                        last_sync_completed_at_local INTEGER,
                        last_success_at_local INTEGER,
                        last_failure_phase TEXT,
                        last_failure_code TEXT,
                        last_local_cursor_revision TEXT,
                        last_server_head_revision TEXT,
                        last_server_head_observed_at INTEGER,
                        last_successful_bootstrap_at INTEGER,
                        last_bootstrap_duration_ms INTEGER,
                        bootstrap_count INTEGER NOT NULL DEFAULT 0,
                        cursor_expiry_count INTEGER NOT NULL DEFAULT 0,
                        last_reconciliation_at INTEGER,
                        last_reconciliation_result TEXT,
                        reconciliation_mismatch_count INTEGER NOT NULL DEFAULT 0,
                        reconciliation_repair_count INTEGER NOT NULL DEFAULT 0,
                        rebootstrap_count INTEGER NOT NULL DEFAULT 0,
                        outbox_conflict_count INTEGER NOT NULL DEFAULT 0,
                        hint_received_count INTEGER NOT NULL DEFAULT 0,
                        hint_trailing_run_count INTEGER NOT NULL DEFAULT 0,
                        hint_dropped_count INTEGER NOT NULL DEFAULT 0,
                        last_realtime_state TEXT,
                        last_realtime_state_at INTEGER,
                        last_failed_realtime_participants INTEGER,
                        updated_at_local INTEGER NOT NULL,
                        PRIMARY KEY(user_id, client_id, org_id, stream)
                    )
                    """.trimIndent(),
                )
            }
        }

        val ROOM_V13_INDEXES: List<String> = listOf(
            "CREATE INDEX IF NOT EXISTS index_invoices_client_id_category ON invoices (client_id, category)",
            "CREATE INDEX IF NOT EXISTS index_payments_invoice_id ON payments (invoice_id)",
            "CREATE INDEX IF NOT EXISTS index_commission_payments_client_id ON commission_payments (client_id)",
            "CREATE INDEX IF NOT EXISTS index_balance_transactions_user_id_created_at ON balance_transactions (user_id, created_at)",
            "CREATE INDEX IF NOT EXISTS index_balance_transactions_sync_status ON balance_transactions (sync_status)",
            "CREATE INDEX IF NOT EXISTS index_withdrawal_requests_user_id_created_at ON withdrawal_requests (user_id, created_at)",
            "CREATE INDEX IF NOT EXISTS index_withdrawal_requests_sync_status ON withdrawal_requests (sync_status)",
            "CREATE INDEX IF NOT EXISTS index_withdrawal_requests_user_id_status ON withdrawal_requests (user_id, status)",
            "CREATE INDEX IF NOT EXISTS index_notifications_user_id_created_at ON notifications (user_id, created_at)",
            "CREATE INDEX IF NOT EXISTS index_notifications_user_id_is_read_read_synced ON notifications (user_id, is_read, read_synced)",
            "CREATE INDEX IF NOT EXISTS index_autodrive_users_user_id ON autodrive_users (user_id)",
            "CREATE INDEX IF NOT EXISTS index_conversations_marketer_id_last_message_at ON conversations (marketer_id, last_message_at)",
            "CREATE INDEX IF NOT EXISTS index_conversations_marketer_id_created_at ON conversations (marketer_id, created_at)",
            "CREATE INDEX IF NOT EXISTS index_conversations_client_id ON conversations (client_id)",
            "CREATE INDEX IF NOT EXISTS index_chat_messages_conversation_id_created_at ON chat_messages (conversation_id, created_at)",
            "CREATE INDEX IF NOT EXISTS index_chat_messages_status_created_at ON chat_messages (status, created_at)",
            "CREATE INDEX IF NOT EXISTS index_chat_messages_sender_type_status_type ON chat_messages (sender_type, status, type)",
            "CREATE INDEX IF NOT EXISTS index_dynamo_content_is_active ON dynamo_content (is_active)",
            "CREATE INDEX IF NOT EXISTS index_weekly_leaderboard_cache_rank ON weekly_leaderboard_cache (rank)",
            "DROP INDEX IF EXISTS index_pending_operations_status_next_retry_at",
            "CREATE INDEX IF NOT EXISTS index_pending_operations_status_next_retry_at_created_at ON pending_operations (status, next_retry_at, created_at)",
        )

        private fun replaceTable(
            database: SupportSQLiteDatabase,
            oldName: String,
            newName: String,
        ) {
            database.execSQL("DROP TABLE $oldName")
            database.execSQL("ALTER TABLE $newName RENAME TO $oldName")
        }

        /**
         * المسار الرسمي المدعوم لترقية قواعد الإنتاج القديمة إلى النسخة الحالية.
         *
         * يجب إضافة أي Migration جديدة هنا وفي AppModule واختبارات الترحيل قبل
         * رفع [AUTODRIVE_DATABASE_VERSION].
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16,
            MIGRATION_16_17,
            MIGRATION_17_18,
            MIGRATION_18_19,
        )
    }
}
