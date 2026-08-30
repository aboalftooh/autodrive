#!/usr/bin/env python3
"""Executable SQLite verification for AutoDrive Room v13 indexes."""

from __future__ import annotations

import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATABASE_FILE = ROOT / "core/database/src/main/kotlin/com/autodrive/app/core/database/AutoDriveDatabase.kt"

TABLES = {
    "invoices": "id TEXT PRIMARY KEY, client_id TEXT NOT NULL, category TEXT NOT NULL, commission TEXT NOT NULL, status TEXT NOT NULL, total_amount TEXT NOT NULL, invoice_number INTEGER NOT NULL, created_at TEXT NOT NULL",
    "payments": "id TEXT PRIMARY KEY, invoice_id TEXT NOT NULL, amount TEXT NOT NULL, created_at TEXT NOT NULL",
    "commission_payments": "id TEXT PRIMARY KEY, client_id TEXT NOT NULL, amount TEXT NOT NULL, note TEXT, invoice_ids TEXT NOT NULL, created_at TEXT NOT NULL",
    "marketer_balance": "id TEXT PRIMARY KEY, user_id TEXT NOT NULL, client_id TEXT NOT NULL, balance TEXT NOT NULL, pending_withdrawal TEXT NOT NULL, updated_at TEXT NOT NULL",
    "balance_transactions": "id TEXT PRIMARY KEY, user_id TEXT NOT NULL, client_id TEXT NOT NULL, type TEXT NOT NULL, amount TEXT NOT NULL, description TEXT NOT NULL, created_at TEXT NOT NULL, sync_status TEXT NOT NULL",
    "withdrawal_requests": "id TEXT PRIMARY KEY, user_id TEXT NOT NULL, client_id TEXT NOT NULL, amount TEXT NOT NULL, status TEXT NOT NULL, bank_name TEXT NOT NULL, bank_account TEXT NOT NULL, transaction_ref TEXT, note TEXT, created_at TEXT NOT NULL, completed_at TEXT, sync_status TEXT NOT NULL",
    "notifications": "id TEXT PRIMARY KEY, user_id TEXT NOT NULL, client_id TEXT NOT NULL, type TEXT NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, is_read INTEGER NOT NULL, created_at TEXT NOT NULL, read_synced INTEGER NOT NULL, nav_route TEXT",
    "autodrive_users": "id TEXT PRIMARY KEY, user_id TEXT NOT NULL, client_id TEXT NOT NULL, org_id TEXT NOT NULL, account_type TEXT NOT NULL, full_name TEXT NOT NULL, phone TEXT NOT NULL, bank_name TEXT, bank_account TEXT, workshop_name TEXT, specialty TEXT, workers_count INTEGER, address TEXT, created_at TEXT NOT NULL, updated_at TEXT NOT NULL, sync_status TEXT NOT NULL",
    "conversations": "id TEXT PRIMARY KEY, marketer_id TEXT NOT NULL, client_id TEXT NOT NULL, title TEXT NOT NULL, subject TEXT NOT NULL, last_message TEXT NOT NULL, last_message_at INTEGER NOT NULL, unread_count INTEGER NOT NULL, created_at INTEGER NOT NULL",
    "chat_messages": "id TEXT PRIMARY KEY, conversation_id TEXT NOT NULL, sender_id TEXT NOT NULL, sender_type TEXT NOT NULL, content TEXT NOT NULL, type TEXT NOT NULL, is_read INTEGER NOT NULL, created_at INTEGER NOT NULL, status TEXT NOT NULL, media_url TEXT, media_mime TEXT, media_duration_ms INTEGER, local_path TEXT",
    "dynamo_content": "id TEXT PRIMARY KEY, content_type TEXT NOT NULL, audience_type TEXT NOT NULL, specialty TEXT NOT NULL, message TEXT NOT NULL, priority INTEGER NOT NULL, is_active INTEGER NOT NULL, created_at TEXT NOT NULL, synced_at INTEGER NOT NULL",
    "weekly_leaderboard_cache": "id TEXT PRIMARY KEY, rank INTEGER NOT NULL, total_amount TEXT NOT NULL, is_me INTEGER NOT NULL, week_number INTEGER NOT NULL, synced_at INTEGER NOT NULL",
    "pending_operations": "id TEXT PRIMARY KEY, table_name TEXT NOT NULL, operation TEXT NOT NULL, payload TEXT NOT NULL, created_at INTEGER NOT NULL, status TEXT NOT NULL, attempt_count INTEGER NOT NULL, next_retry_at INTEGER NOT NULL, last_error_code TEXT, last_error_message TEXT, payload_version INTEGER NOT NULL, idempotency_key TEXT NOT NULL",
}

EXPECTED_QUERY_PLANS = {
    "invoice owner/category": (
        "SELECT * FROM invoices WHERE client_id = ? AND category = 'SALE' AND CAST(commission AS NUMERIC) > 0",
        ("client-1",),
        "index_invoices_client_id_category",
    ),
    "payment invoice": (
        "SELECT * FROM payments WHERE invoice_id IN (?)",
        ("invoice-1",),
        "index_payments_invoice_id",
    ),
    "commission owner": (
        "SELECT * FROM commission_payments WHERE client_id = ?",
        ("client-1",),
        "index_commission_payments_client_id",
    ),
    "balance timeline": (
        "SELECT * FROM balance_transactions WHERE user_id = ? ORDER BY created_at DESC LIMIT 50",
        ("user-1",),
        "index_balance_transactions_user_id_created_at",
    ),
    "balance pending": (
        "SELECT * FROM balance_transactions WHERE sync_status = 'PENDING'",
        (),
        "index_balance_transactions_sync_status",
    ),
    "withdrawal timeline": (
        "SELECT * FROM withdrawal_requests WHERE user_id = ? ORDER BY created_at DESC LIMIT 20",
        ("user-1",),
        "index_withdrawal_requests_user_id_created_at",
    ),
    "withdrawal pending": (
        "SELECT * FROM withdrawal_requests WHERE sync_status = 'PENDING'",
        (),
        "index_withdrawal_requests_sync_status",
    ),
    "withdrawal user/status": (
        "DELETE FROM withdrawal_requests WHERE user_id = ? AND status = 'PENDING'",
        ("user-1",),
        "index_withdrawal_requests_user_id_status",
    ),
    "notification timeline": (
        "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50",
        ("user-1",),
        "index_notifications_user_id_created_at",
    ),
    "notification unread": (
        "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0",
        ("user-1",),
        "index_notifications_user_id_is_read_read_synced",
    ),
    "current user": (
        "SELECT * FROM autodrive_users WHERE user_id = ? LIMIT 1",
        ("user-1",),
        "index_autodrive_users_user_id",
    ),
    "conversation recent": (
        "SELECT * FROM conversations WHERE marketer_id = ? ORDER BY last_message_at DESC",
        ("marketer-1",),
        "index_conversations_marketer_id_last_message_at",
    ),
    "conversation created": (
        "SELECT * FROM conversations WHERE marketer_id = ? ORDER BY created_at DESC LIMIT 1",
        ("marketer-1",),
        "index_conversations_marketer_id_created_at",
    ),
    "conversation client": (
        "SELECT * FROM conversations WHERE client_id = ? LIMIT 1",
        ("client-1",),
        "index_conversations_client_id",
    ),
    "chat timeline": (
        "SELECT * FROM chat_messages WHERE conversation_id = ? ORDER BY created_at ASC",
        ("conversation-1",),
        "index_chat_messages_conversation_id_created_at",
    ),
    "chat retry": (
        "SELECT * FROM chat_messages WHERE status = ? ORDER BY created_at ASC",
        ("FAILED",),
        "index_chat_messages_status_created_at",
    ),
    "chat admin media": (
        "SELECT * FROM chat_messages WHERE sender_type = 'ADMIN' AND status = 'SENT' AND type = 'IMAGE'",
        (),
        "index_chat_messages_sender_type_status_type",
    ),
    "active content": (
        "SELECT COUNT(*) FROM dynamo_content WHERE is_active = 1",
        (),
        "index_dynamo_content_is_active",
    ),
    "leaderboard rank": (
        "SELECT * FROM weekly_leaderboard_cache ORDER BY rank ASC",
        (),
        "index_weekly_leaderboard_cache_rank",
    ),
    "outbox due": (
        "SELECT * FROM pending_operations WHERE status = 'PENDING' AND next_retry_at <= ? ORDER BY created_at ASC LIMIT 20",
        (1000,),
        "index_pending_operations_status_next_retry_at_created_at",
    ),
}


def migration_sql() -> list[str]:
    source = DATABASE_FILE.read_text(encoding="utf-8")
    match = re.search(
        r"ROOM_V13_INDEXES: List<String> = listOf\((.*?)\n\s*\)\n\n\s*private fun replaceTable",
        source,
        flags=re.S,
    )
    if not match:
        raise AssertionError("ROOM_V13_INDEXES block not found")
    return re.findall(r'"([^"]+)"', match.group(1))


def create_schema(connection: sqlite3.Connection) -> None:
    for table, columns in TABLES.items():
        connection.execute(f"CREATE TABLE {table} ({columns})")
    connection.execute(
        "CREATE UNIQUE INDEX index_marketer_balance_user_id ON marketer_balance (user_id)"
    )
    connection.execute(
        "CREATE UNIQUE INDEX index_pending_operations_idempotency_key ON pending_operations (idempotency_key)"
    )
    connection.execute(
        "CREATE INDEX index_pending_operations_status_next_retry_at ON pending_operations (status, next_retry_at)"
    )


def seed(connection: sqlite3.Connection) -> None:
    connection.execute("INSERT INTO invoices VALUES ('invoice-1','client-1','SALE','0.10','OPEN','100.00',1,'2026-01-01')")
    connection.execute("INSERT INTO payments VALUES ('payment-1','invoice-1','10.00','2026-01-01')")
    connection.execute("INSERT INTO commission_payments VALUES ('commission-1','client-1','5.00',NULL,'invoice-1','2026-01-01')")
    connection.execute("INSERT INTO marketer_balance VALUES ('balance-1','user-1','client-1','95.00','5.00','2026-01-01')")
    connection.execute("INSERT INTO balance_transactions VALUES ('transaction-1','user-1','client-1','CREDIT','5.00','seed','2026-01-01','PENDING')")
    connection.execute("INSERT INTO withdrawal_requests VALUES ('withdrawal-1','user-1','client-1','5.00','PENDING','bank','account',NULL,NULL,'2026-01-01',NULL,'PENDING')")
    connection.execute("INSERT INTO notifications VALUES ('notification-1','user-1','client-1','TYPE','title','body',0,'2026-01-01',0,NULL)")
    connection.execute("INSERT INTO autodrive_users VALUES ('profile-1','user-1','client-1','org-1','MARKETER','User','249000000000',NULL,NULL,NULL,NULL,NULL,NULL,'2026-01-01','2026-01-01','SYNCED')")
    connection.execute("INSERT INTO conversations VALUES ('conversation-1','marketer-1','client-1','title','subject','last',10,0,1)")
    connection.execute("INSERT INTO chat_messages VALUES ('message-1','conversation-1','marketer-1','ADMIN','body','IMAGE',0,1,'SENT','https://example',NULL,NULL,NULL)")
    connection.execute("INSERT INTO dynamo_content VALUES ('content-1','TIP','ALL','general','message',1,1,'2026-01-01',1)")
    connection.execute("INSERT INTO weekly_leaderboard_cache VALUES ('rank-1',1,'100.00',1,1,1)")
    connection.execute("INSERT INTO pending_operations VALUES ('op-1','withdrawal_requests','REQUEST','{}',1,'PENDING',0,0,NULL,NULL,1,'idem-1')")


def explain(connection: sqlite3.Connection, sql: str, args: tuple[object, ...]) -> str:
    rows = connection.execute("EXPLAIN QUERY PLAN " + sql, args).fetchall()
    return " | ".join(str(row[3]) for row in rows)


def main() -> None:
    connection = sqlite3.connect(":memory:")
    create_schema(connection)
    seed(connection)
    before = {table: connection.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0] for table in TABLES}

    statements = migration_sql()
    for statement in statements:
        connection.execute(statement)

    after = {table: connection.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0] for table in TABLES}
    assert before == after, f"Row counts changed: before={before}, after={after}"

    index_names = {
        row[0]
        for row in connection.execute(
            "SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'index_%'"
        )
    }
    created = {
        match.group(1)
        for statement in statements
        if (match := re.search(r"CREATE (?:UNIQUE )?INDEX IF NOT EXISTS ([^ ]+)", statement))
    }
    missing = created - index_names
    assert not missing, f"Missing indexes after migration: {sorted(missing)}"
    assert "index_pending_operations_status_next_retry_at" not in index_names

    for label, (sql, args, index_name) in EXPECTED_QUERY_PLANS.items():
        plan = explain(connection, sql, args)
        assert index_name in plan, f"{label}: expected {index_name}, got {plan}"

    print(f"migration statements: {len(statements)}/{len(statements)} PASS")
    print(f"rows preserved: {len(TABLES)}/{len(TABLES)} PASS")
    print(f"indexes created: {len(created)}/{len(created)} PASS")
    print(f"query plans: {len(EXPECTED_QUERY_PLANS)}/{len(EXPECTED_QUERY_PLANS)} PASS")


if __name__ == "__main__":
    main()
