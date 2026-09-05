#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
checks: list[dict[str, object]] = []

def text(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")

def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def add(name: str, passed: bool, detail: str = "") -> None:
    checks.append({"name": name, "passed": bool(passed), "detail": detail})

db_rel = "core/database/src/main/kotlin/com/autodrive/app/core/database/AutoDriveDatabase.kt"
db = text(db_rel)
inbox_entity = text("core/database/src/main/kotlin/com/autodrive/app/core/database/entities/SyncInboxEntity.kt")
inbox_dao = text("core/database/src/main/kotlin/com/autodrive/app/core/database/dao/SyncInboxDao.kt")
deletion = text("core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/DeletionSynchronizer.kt")
legacy = text("core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/LegacyRemotePuller.kt")
cleaner = text("core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/LocalDataCleaner.kt")
manager = text("core/sync/src/main/kotlin/com/autodrive/app/core/sync/realtime/RealtimeManager.kt")
observer = text("core/sync/src/main/kotlin/com/autodrive/app/core/sync/domain/RealtimeConnectionObserver.kt")
participant_contract = text("core/sync/src/main/kotlin/com/autodrive/app/core/sync/realtime/RealtimeParticipant.kt")
dispatcher = text("core/sync/src/main/kotlin/com/autodrive/app/core/sync/realtime/RealtimeHintDispatcher.kt")
coordinator = text("core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/DefaultSyncCoordinator.kt")
sync_manager = text("core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/SyncManager.kt")
pending = text("core/database/src/main/kotlin/com/autodrive/app/core/database/entities/Entities.kt")
outbox = text("core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/OutboxSynchronizer.kt")

# Room / migration
add("01_room_is_16", "AUTODRIVE_DATABASE_VERSION = 16" in db)
add("02_sync_inbox_entity_registered", "SyncInboxEntity::class" in db and "abstract fun syncInboxDao()" in db)
add("03_migration_15_16_present", "val MIGRATION_15_16 = object : Migration(15, 16)" in db)
add("04_migration_registered", "MIGRATION_15_16," in db)
add("05_inbox_table_created", "CREATE TABLE IF NOT EXISTS sync_inbox" in db)
add("06_inbox_pk_scoped", "PRIMARY KEY(user_id, client_id, org_id, stream, event_id)" in db)
add("07_inbox_server_revision_nullable", "server_revision TEXT," in db and "server_revision TEXT NOT NULL" not in db)
add("08_inbox_transaction_group_nullable", "transaction_group_id TEXT," in db and "transaction_group_id TEXT NOT NULL" not in db)
add("09_inbox_no_raw_payload", not re.search(r"\b(payload|raw_payload|body)\b", inbox_entity, re.I))
add("10_no_destructive_fallback", "fallbackToDestructiveMigration" not in db)

# Historical migration immutability
m13 = db[db.index("        val MIGRATION_13_14"):db.index("        /**\n         * v68")]
m14 = db[db.index("        val MIGRATION_14_15"):db.index("        /** v70: durable scoped inbound event ledger.")]
add("11_migration_13_14_unchanged", sha256_bytes(m13.encode()) == "079d3c00ea43a453db6acaea822c730ef4bb2f4b4eb22b3b80bfffd422c70a8f")
add("12_migration_14_15_unchanged", sha256_bytes(m14.encode()) == "c151c823af07f1145e416137060932bf8fcc4bfa449f4b0d0f8e728c5dbfa981")
sql69 = ROOT / "supabase/migrations/20260821203000_autodrive_idempotent_commands_v1.sql"
add("13_v69_server_migration_unchanged", sha256_bytes(sql69.read_bytes()) == "6663381c4bf177c7cc22c75fb4c1eee1683290894307ec9ade85e4fe7620c01e")
server_migrations = sorted((ROOT / "supabase/migrations").glob("*.sql"))
add("14_no_new_server_migration", len(server_migrations) == 4, str(len(server_migrations)))

# Inbox DAO / scoped identity
for idx, col in enumerate(("user_id", "client_id", "org_id", "stream", "event_id"), start=15):
    add(f"{idx:02d}_inbox_identity_{col}", col in inbox_entity)
add("20_inbox_get_exact_scope", all(token in inbox_dao for token in ("user_id = :userId", "client_id = :clientId", "org_id = :orgId", "stream = :stream", "event_id = :eventId")))
add("21_inbox_insert_abort", "OnConflictStrategy.ABORT" in inbox_dao)
add("22_inbox_mark_applied_scoped", "UPDATE sync_inbox SET applied_at = :appliedAt" in inbox_dao and "event_id = :eventId" in inbox_dao)
add("23_inbox_logout_exact_scope", "db.syncInboxDao().deleteForScope(scope.userId, scope.clientId, scope.orgId)" in cleaner)
add("24_no_global_inbox_delete", "DELETE FROM sync_inbox" in inbox_dao and "WHERE user_id = :userId" in inbox_dao and "deleteAll" not in inbox_dao)

# Atomic event-bearing deletion apply
add("25_deletion_room_transaction", "db.withTransaction" in deletion)
add("26_deletion_scope_recheck_inside_transaction", "requireCurrentScope(scope)" in deletion)
add("27_deletion_inbox_lookup", "db.syncInboxDao().get" in deletion)
add("28_deletion_identity_conflict_fail_closed", "INBOX_EVENT_IDENTITY_CONFLICT" in deletion)
add("29_deletion_apply_then_mark", deletion.index("applyDeletion(scope, deletion)") < deletion.index("db.syncInboxDao().markApplied"))
add("30_cursor_after_inbox_apply", deletion.index("db.syncInboxDao().markApplied") < deletion.index("db.syncCursorDao().upsert"))
add("31_network_before_transaction", deletion.index("feed.changesSince") < deletion.index("db.withTransaction"))
add("32_unknown_entity_fails_closed", 'throw TombstoneValidationException("UNKNOWN_ENTITY_TYPE")' in deletion)
add("33_nonadvancing_cursor_guard", "NON_ADVANCING_CURSOR" in deletion)

# No synthetic authority in snapshots
add("34_legacy_declares_snapshot_compat", "Compatibility positive-row pulls" in legacy and "no eventId/serverRevision is synthesized" in legacy)
add("35_legacy_no_inbox_event_fabrication", "SyncInboxEntity" not in legacy and "syncInboxDao" not in legacy)
add("36_legacy_no_server_revision_assignment", "serverRevision =" not in legacy)
add("37_snapshot_scope_recheck", "SyncScope.from(sessionReader.currentSession()) != scope" in legacy)
add("38_snapshot_room_transaction", "db.withTransaction" in legacy)
add("39_billing_fetches_both_before_apply", legacy.index('postgrest["invoices"]') < legacy.index('postgrest["payments"]') < legacy.index("snapshotTransaction(scope) {\n            db.invoiceDao().upsertAll"))
add("40_billing_invoice_payment_one_transaction", "db.invoiceDao().upsertAll(invoiceEntities)\n            db.paymentDao().upsertAll(paymentEntities)" in legacy)
add("41_chat_fetch_before_apply", legacy.index('postgrest["internal_messages"]') < legacy.index("snapshotTransaction(scope) {", legacy.index("private suspend fun pullChat")))
add("42_absence_not_delete_documented", "absence is never interpreted as deletion" in legacy)

# Realtime hint-only
participant_paths = [
    "feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/data/realtime/BillingRealtimeParticipant.kt",
    "feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/data/realtime/BalanceRealtimeParticipant.kt",
    "feature/notifications/src/main/kotlin/com/autodrive/app/feature/notifications/data/realtime/NotificationsRealtimeParticipant.kt",
    "feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/data/realtime/ChatRealtimeParticipant.kt",
]
participant_sources = {p: text(p) for p in participant_paths}
for i, (path, src) in enumerate(participant_sources.items(), start=43):
    ok = all(t in src for t in ("PostgresAction.Insert", "PostgresAction.Update", "PostgresAction.Delete", "hints.requestSync()"))
    add(f"{i:02d}_{Path(path).stem}_all_events_hint", ok)
forbidden = ("AutoDriveDatabase", "oldRecord", "LocalNotificationPublisher", "BillingTargetedRefresher", ".upsert(", ".deleteBy", ".insert(", ".update")
for i, (path, src) in enumerate(participant_sources.items(), start=47):
    hits = [token for token in forbidden if token in src]
    add(f"{i:02d}_{Path(path).stem}_no_business_write", not hits, ",".join(hits))
add("51_realtime_direct_room_write_count_zero", all("AutoDriveDatabase" not in s for s in participant_sources.values()))
add("52_realtime_transitive_refresher_count_zero", all("BillingTargetedRefresher" not in s for s in participant_sources.values()))
add("53_realtime_oldrecord_authority_zero", all("oldRecord" not in s for s in participant_sources.values()))
add("54_realtime_payload_side_effect_zero", all("LocalNotificationPublisher" not in s for s in participant_sources.values()))
add("55_hint_dispatches_realtime_reason", "SyncReason.REALTIME_HINT" in dispatcher and "requestSync" in dispatcher)

# Aggregate health
add("56_aggregate_degraded_present", "DEGRADED" in observer)
add("57_participant_required_explicit", "val required: Boolean get() = true" in participant_contract)
add("58_participant_health_tracked", "participantHealth" in manager and "ParticipantHealth.HEALTHY" in manager)
add("59_connected_requires_all_required", "states.all { it == ParticipantHealth.HEALTHY }" in manager)
add("60_partial_required_is_degraded", "states.any { it == ParticipantHealth.HEALTHY } -> RealtimeAggregateHealth.DEGRADED" in manager)
add("61_first_subscriber_bug_removed", "subscribed.receive()" not in manager and "Channel<" not in manager)
add("62_public_connected_never_maps_degraded", "RealtimeAggregateHealth.DEGRADED -> RealtimeConnectionState.CONNECTING" in manager)

# Inherited foundations
add("63_requested_generation_preserved", "requestedGeneration" in coordinator)
add("64_completed_generation_preserved", "completedGeneration" in coordinator)
add("65_completion_edge_lock_preserved", "activeSync === shared" in coordinator and "shared.complete(lastResult)" in coordinator)
push_pos = sync_manager.find("outboxSynchronizer.flush(scope")
pull_pos = sync_manager.find("remotePuller.pull(scope")
add("66_push_before_pull_preserved", 0 <= push_pos < pull_pos)
for i, field in enumerate(("mutationId", "userId", "clientId", "orgId", "leaseUntil"), start=67):
    add(f"{i:02d}_v68_pending_{field}", f"val {field}" in pending)
add("72_v68_outbox_transaction_preserved", "withTransaction" in outbox)
receipt = text("core/sync/src/main/kotlin/com/autodrive/app/core/sync/outbox/CommandReceipt.kt") if (ROOT / "core/sync/src/main/kotlin/com/autodrive/app/core/sync/outbox/CommandReceipt.kt").exists() else ""
# v69 verifier itself remains the canonical exhaustive command check; ensure its source and receipt markers still exist.
add("73_v69_static_verifier_preserved", (ROOT / "scripts/verify-v69-static.py").exists())
add("74_v69_model_verifier_preserved", (ROOT / "scripts/verify-v69-model.py").exists())
add("75_command_receipt_revision_not_used_by_deletion", "CommandReceipt" not in deletion and "serverRevision" not in text("core/database/src/main/kotlin/com/autodrive/app/core/database/entities/SyncCursorEntity.kt"))

# UI drift aggregate from v69 baseline: presentation + design system production Kotlin.
ui_paths = []
for p in ROOT.rglob("*.kt"):
    rel = p.relative_to(ROOT).as_posix()
    if "/src/main/" not in rel:
        continue
    if "/presentation/" in rel or "/designsystem/" in rel:
        ui_paths.append((rel, p))
h = hashlib.sha256()
for rel, p in sorted(ui_paths):
    h.update(rel.encode() + b"\0" + hashlib.sha256(p.read_bytes()).digest())
add("76_production_ui_drift_zero", len(ui_paths) == 90 and h.hexdigest() == "85a03f42baa35909ac8404ad84355ba87e6250839dd149783c50883de8326f05", f"{len(ui_paths)}:{h.hexdigest()}")

# No obvious waiver/new destructive escape hatch
all_prod = "\n".join(p.read_text(encoding="utf-8", errors="ignore") for p in ROOT.rglob("*.kt") if "/src/main/" in p.as_posix())
add("77_no_new_v70_waiver_marker", "V70_WAIVER" not in all_prod)
add("78_no_fallback_destructive_anywhere", "fallbackToDestructiveMigration" not in all_prod)

passed = sum(1 for c in checks if c["passed"])
result = {
    "allPassed": passed == len(checks),
    "passedCount": passed,
    "checkCount": len(checks),
    "checks": checks,
}
print(json.dumps(result, ensure_ascii=False, sort_keys=True, separators=(",", ":")))
raise SystemExit(0 if result["allPassed"] else 1)
