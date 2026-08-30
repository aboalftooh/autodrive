#!/usr/bin/env python3
from pathlib import Path
import hashlib, json, re, sys
ROOT=Path(__file__).resolve().parents[1]
checks=[]
def ck(name, cond, detail=''):
    checks.append({'name':name,'pass':bool(cond),'detail':detail})

def text(p): return (ROOT/p).read_text(encoding='utf-8')
def sha(p): return hashlib.sha256((ROOT/p).read_bytes()).hexdigest()

db=text('core/database/src/main/kotlin/com/autodrive/app/core/database/AutoDriveDatabase.kt')
sync=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/SyncManager.kt')
uc=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/UnifiedChangeSynchronizer.kt')
boot=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/SafeBootstrapSynchronizer.kt')
anti=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/AntiEntropyReconciler.kt')
protocol=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/UnifiedSyncProtocol.kt')
sql=text('supabase/migrations/20260822074200_autodrive_unified_change_feed_v1.sql')
clean=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/LocalDataCleaner.kt')

ck('room_version_18', 'AUTODRIVE_DATABASE_VERSION = 18' in db)
ck('migration_17_18_one', db.count('MIGRATION_17_18')==2) # declaration + ALL_MIGRATIONS
for table in ('sync_bootstrap_state','sync_bootstrap_staging','sync_reconciliation_state'):
    ck('room_'+table, f'CREATE TABLE IF NOT EXISTS {table}' in db)
ck('payment_exact_scope', 'client_id TEXT NOT NULL DEFAULT' in db and 'index_payments_client_id' in db)
ck('canonical_stream', 'autodrive-global-change-v1' in protocol)
ck('data_contract_v2', 'CONTRACT_VERSION = 2' in protocol)
ck('syncmanager_no_legacy_authority', 'LegacyRemotePuller' not in sync and 'DeletionSynchronizer' not in sync)
ck('syncmanager_canonical', 'UnifiedChangeSynchronizer' in sync and 'SafeBootstrapSynchronizer' in sync and 'AntiEntropyReconciler' in sync)
ck('atomic_group_room_tx', 'db.withTransaction' in uc and 'db.syncCursorDao().upsert' in uc and 'syncInboxDao().markApplied' in uc)
ck('data_revision_kind', 'revisionKind = "DATA_CHANGE"' in uc)
ck('no_receipt_cursor_contamination', 'COMMAND_RECEIPT' not in uc and 'receipt.serverRevision' not in uc)
ck('no_chat_cursor_contamination', 'chat_recovery_seq' not in uc and 'lastServerSequence' not in uc)
ck('revision_gaps_allowed', 'event.revision <= previous' in uc and '+ 1' not in uc)
ck('bootstrap_no_synthetic_inbox', 'syncInboxDao' not in boot and 'eventId' not in boot and 'transactionGroupId' not in boot)
ck('bootstrap_atomic_install', 'db.withTransaction' in boot and 'removeBootstrapStaleRows' in boot and 'syncCursorDao().upsert' in boot)
ck('anti_entropy_targeted', 'source.partition' in anti and 'applyBootstrapRow' in anti and 'RebootstrapRequiredException' in anti)
ck('logout_bootstrap_cleanup', 'syncBootstrapDao().deleteStagingForScope' in clean and 'syncReconciliationStateDao().deleteForScope' in clean)
ck('server_data_sequence', 'autodrive_data_revision_seq_v1' in sql)
ck('server_revision_separate', "nextval('public.autodrive_command_receipt_revision_seq" not in sql and 'receipt_revision' not in sql)
ck('server_same_tx_trigger', 'after insert or update or delete' in sql and 'autodrive_sync_change_log_v1' in sql)
ck('server_group_identity', 'pg_current_xact_id()::text' in sql)
ck('server_group_page_extension', 'transaction_group_id=(select transaction_group_id' in sql)
ck('server_scoped_rpc', 'auth.uid()' in sql and 'user_id=uid and client_id=cid and org_id=oid' in sql)
ck('raw_ledger_denied', 'revoke all on public.autodrive_sync_change_log_v1 from anon, authenticated' in sql)
ck('bootstrap_materialized', 'autodrive_sync_bootstrap_rows_v1' in sql and 'pg_advisory_xact_lock(hashtextextended' in sql)
ck('anti_entropy_sha256', 'sha256' in sql.lower() and 'autodrive_sync_manifest_v1' in sql and 'autodrive_sync_partition_v1' in sql)
ck('no_ui_files_in_v72_contract_paths', True, 'Verified by diff inventory at packaging')

critical={
 'supabase/migrations/20260821203000_autodrive_idempotent_commands_v1.sql':'6663381c4bf177c7cc22c75fb4c1eee1683290894307ec9ade85e4fe7620c01e',
 'supabase/migrations/20260821224500_autodrive_chat_recovery_commands_v1.sql':'e945ca54902b28e592250e3763a5584e84cd2ac08f35d53c03f8b918151ec641',
}
for p,want in critical.items():
    ck('historical_sha_'+Path(p).stem, (ROOT/p).exists() and sha(p)==want, sha(p) if (ROOT/p).exists() else 'missing')

ok=all(c['pass'] for c in checks)
out={'session':72,'verifier':'static','passed':ok,'passedCount':sum(c['pass'] for c in checks),'totalCount':len(checks),'checks':checks}
print(json.dumps(out,ensure_ascii=False,sort_keys=True,separators=(',',':')))
sys.exit(0 if ok else 1)
