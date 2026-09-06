#!/usr/bin/env python3
from pathlib import Path
import hashlib, json, re, sys
ROOT = Path(__file__).resolve().parents[1]
checks=[]
def ck(name, cond, detail=''):
    checks.append((name, bool(cond), detail))
def text(rel): return (ROOT/rel).read_text(encoding='utf-8')

db=text('core/database/src/main/kotlin/com/autodrive/app/core/database/AutoDriveDatabase.kt')
outbox=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/OutboxSynchronizer.kt')
retry=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/outbox/OutboxRetryPolicy.kt')
receipt=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/outbox/PendingOperationProcessor.kt')
gateway=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/outbox/IdempotentServerCommandGateway.kt')
contracts=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/outbox/OutboxContracts.kt')
entity=text('core/database/src/main/kotlin/com/autodrive/app/core/database/entities/Entities.kt')
dao=text('core/database/src/main/kotlin/com/autodrive/app/core/database/dao/PendingOperationDao.kt')
coord=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/DefaultSyncCoordinator.kt')
syncmgr=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/SyncManager.kt')
push=text('core/platform/src/main/kotlin/com/autodrive/app/core/platform/notifications/PushTokenRepository.kt')
balance=text('feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/data/BalanceRepositoryImpl.kt')
mig=ROOT/'supabase/migrations/20260821203000_autodrive_idempotent_commands_v1.sql'
sql=mig.read_text()

# Room / v68 foundations
ck('01_room_is_15', 'const val AUTODRIVE_DATABASE_VERSION = 15' in db)
ck('02_no_room_16_migration', 'Migration(15, 16)' not in db and 'MIGRATION_15_16' not in db)
for fld in ['userId','clientId','orgId','mutationId','leaseUntil']:
    ck(f'v68_pending_field_{fld}', re.search(rf'val {fld}\b', entity) is not None)
ck('08_scoped_due_query', all(x in dao for x in ['user_id = :userId','client_id = :clientId','org_id = :orgId']))
ck('09_scoped_finalize_delete', 'deleteClaimedById' in dao and 'user_id = :userId' in dao)
ck('10_lease_retry_separated', 'lease_until' in dao and 'next_retry_at' in dao)
ck('11_outbox_contract_stays_v1', 'OUTBOX_CONTRACT_VERSION = 1' in contracts)
ck('12_generation_requested_preserved', 'requestedGeneration' in coord)
ck('13_generation_completed_preserved', 'completedGeneration' in coord)
ck('14_completion_edge_mutex_preserved', 'requestedGeneration <= completedGeneration' in coord)
# Use phase calls rather than comments.
recover_pos=syncmgr.find('recoverExpiredLeases')
push_pos=syncmgr.find('outboxSynchronizer.flush')
pull_pos=syncmgr.find('remotePuller.pull')
ck('15_recover_before_push', recover_pos >= 0 and push_pos > recover_pos)
ck('16_push_before_pull', push_pos >= 0 and pull_pos > push_pos)

# Receipt model / validation
for fld in ['mutationId','commandType','resultStatus','serverEntityId','serverRevision','revisionKind','replayed','errorCode']:
    ck(f'receipt_field_{fld}', re.search(rf'val {fld}\b', receipt) is not None)
ck('25_receipt_mutation_validated', 'INVALID_SERVER_RECEIPT_MUTATION' in outbox)
ck('26_receipt_command_validated', 'INVALID_SERVER_RECEIPT_COMMAND' in outbox)
ck('27_receipt_revision_kind_validated', 'INVALID_SERVER_RECEIPT_REVISION_KIND' in outbox)
ck('28_receipt_entity_validated', 'INVALID_SERVER_RECEIPT_ENTITY' in outbox)
ck('29_unknown_receipt_rejected', 'UNSUPPORTED_SERVER_RECEIPT' in outbox)
ck('30_receipt_revision_not_written_to_cursor', 'syncCursor' not in outbox and 'SyncCursor' not in gateway)

# Android command routing
for rpc in [
 'autodrive_update_profile_command_v1','autodrive_request_withdrawal_command_v1',
 'autodrive_send_chat_message_command_v1','autodrive_mark_chat_read_command_v1',
 'autodrive_mark_notification_read_command_v1']:
    ck('rpc_'+rpc, rpc in gateway)
ck('36_profile_direct_outbox_write_removed', 'postgrest["autodrive_users"]' not in outbox)
ck('37_chat_direct_outbox_insert_removed', 'internal_messages' not in outbox or '.insert(' not in outbox)
ck('38_notification_direct_outbox_update_removed', 'postgrest["notifications"]' not in outbox)
ck('39_withdrawal_legacy_rpc_removed_from_outbox', '"request_withdrawal"' not in outbox)
ck('40_push_register_receipted', 'autodrive_register_push_token_command_v1' in push)
ck('41_push_revoke_receipted', 'autodrive_revoke_push_token_command_v1' in push)
ck('42_push_direct_table_mutation_removed', 'postgrest["push_tokens"]' not in push)
ck('43_cancel_withdrawal_receipted', 'autodrive_cancel_pending_withdrawals_command_v1' in balance)
ck('44_cancel_legacy_rpc_removed', '.rpc("cancel_pending_withdrawals"' not in balance)

# Typed retry
for cat in ['TRANSIENT','AUTH','PERMISSION','VALIDATION','CONFLICT','ALREADY_COMMITTED','AMBIGUOUS','PERMANENT_PROTOCOL']:
    ck('retry_category_'+cat, cat in retry)
ck('53_http_401_auth', '401 -> OutboxFailureCategory.AUTH' in retry)
ck('54_http_403_permission', '403 -> OutboxFailureCategory.PERMISSION' in retry)
ck('55_http_409_conflict', '409 -> OutboxFailureCategory.CONFLICT' in retry)
ck('56_http_422_validation', '400, 404, 422 -> OutboxFailureCategory.VALIDATION' in retry)
ck('57_http_5xx_transient', 'in 500..599 -> OutboxFailureCategory.TRANSIENT' in retry)
ck('58_unknown_ambiguous', 'UNCLASSIFIED_AMBIGUOUS' in retry)
# Decision authority must not inspect message text.
decision_region=retry[retry.find('object OutboxErrorClassifier'):]
ck('59_no_message_contains_decision', '.contains(' not in decision_region and 'lowercase(' not in decision_region)
ck('60_ambiguous_not_terminal_by_attempt_count', 'OutboxFailureCategory.AMBIGUOUS -> false' in retry)

# Server receipt infrastructure
ck('61_server_migration_present', mig.exists())
ck('62_receipt_ledger_present', 'create table if not exists public.autodrive_command_receipts' in sql)
ck('63_receipt_scope_primary_key', 'primary key (user_id, client_id, org_id, mutation_id)' in sql)
ck('64_scope_derived_from_auth', 'v_user_id uuid := auth.uid()' in sql and 'from public.autodrive_users' in sql)
ck('65_server_fingerprint_sha256', "extensions.digest" in sql and "'sha256'" in sql)
ck('66_server_fingerprint_not_client_param', 'p_request_fingerprint' not in sql[sql.find('autodrive_update_profile_command_v1'):sql.find('-- REQUEST_WITHDRAWAL')])
ck('67_advisory_duplicate_lock', 'pg_advisory_xact_lock' in sql)
ck('68_same_mutation_replay_path', 'autodrive_command_existing_or_conflict_v1' in sql and 'p_replayed' in sql)
ck('69_changed_payload_conflict', 'MUTATION_ID_REUSE_CONFLICT' in sql)
ck('70_receipt_revision_kind', "revision_kind text not null default 'COMMAND_RECEIPT'" in sql)
ck('71_receipt_revision_not_global_cursor_comment', 'MUST NOT be used as the global sync/change-feed cursor' in sql)
ck('72_no_raw_payload_column', not re.search(r'\b(payload|request_payload|body|token)\s+(jsonb|text)', sql[sql.find('create table if not exists public.autodrive_command_receipts'):sql.find(');',sql.find('create table if not exists public.autodrive_command_receipts'))]))
ck('73_receipt_table_rls_enabled', 'alter table public.autodrive_command_receipts enable row level security' in sql)
ck('74_receipt_table_no_authenticated_grant', 'grant ' not in '\n'.join(l.lower() for l in sql.splitlines() if 'autodrive_command_receipts' in l.lower() and 'table' in l.lower()))
ck('75_receipt_retention_safe_default', 'No automatic cleanup is installed' in sql)
ck('76_profile_rpc_server', 'autodrive_update_profile_command_v1' in sql)
ck('77_withdrawal_rpc_server', 'autodrive_request_withdrawal_command_v1' in sql and 'client_request_id' in sql)
ck('78_withdrawal_no_error_text_parsing', 'SQLERRM' not in sql and 'message_text' not in sql)
ck('79_chat_send_rpc_server', 'autodrive_send_chat_message_command_v1' in sql and 'on conflict (id) do nothing' in sql)
ck('80_chat_changed_payload_guard', 'MESSAGE_MUTATION_ID_MISMATCH' in sql and 'MUTATION_ID_REUSE_CONFLICT' in sql)
ck('81_chat_read_rpc_server', 'autodrive_mark_chat_read_command_v1' in sql)
ck('82_notification_read_rpc_server', 'autodrive_mark_notification_read_command_v1' in sql)
ck('83_push_register_rpc_server', 'autodrive_register_push_token_command_v1' in sql)
ck('84_push_revoke_rpc_server', 'autodrive_revoke_push_token_command_v1' in sql)
ck('85_cancel_rpc_server', 'autodrive_cancel_pending_withdrawals_command_v1' in sql)
ck('86_receipt_lookup_private_rpc', 'autodrive_get_command_receipt_v1' in sql and 'from public.autodrive_command_receipts' in sql)
ck('87_no_dynamic_sql_router', 'execute format(' not in sql.lower() and 'execute_command(' not in sql.lower())
ck('88_no_service_role_android', not any('service_role' in '\n'.join(line for line in p.read_text(errors='ignore').lower().splitlines() if not line.lstrip().startswith(('/', '*'))) for base in [ROOT/'core',ROOT/'feature',ROOT/'app/src/main'] for p in base.rglob('*.kt')))

# Scope / drift gates
migrations=sorted((ROOT/'supabase/migrations').glob('*.sql'))
ck('89_exactly_one_new_v69_server_migration', sum('20260821203000_autodrive_idempotent_commands_v1.sql' in str(p) for p in migrations)==1)
ck('90_no_v69_room_migration_file', not any('v69' in p.name.lower() and 'room' in p.name.lower() for p in ROOT.rglob('*')))
ui_changed_marker = ROOT/'verification-v69/ui-drift-count.txt'
ck('91_ui_drift_zero', ui_changed_marker.exists() and ui_changed_marker.read_text().strip()=='0')
hist_marker = ROOT/'verification-v69/historical-migration-mutation-count.txt'
ck('92_historical_migration_mutation_zero', hist_marker.exists() and hist_marker.read_text().strip()=='0')
ck('93_no_new_waiver_marker', not (ROOT/'verification-v69/WAIVER.md').exists())

passed=sum(1 for _,ok,_ in checks if ok)
for name,ok,detail in checks:
    print(('PASS' if ok else 'FAIL'), name, detail)
print(f'SUMMARY {passed}/{len(checks)} PASS')
result={'passed':passed,'total':len(checks),'failures':[n for n,o,_ in checks if not o]}
(ROOT/'verification-v69/v69-static-result.json').write_text(json.dumps(result,indent=2)+'\n')
sys.exit(0 if passed==len(checks) else 1)
