#!/usr/bin/env python3
from pathlib import Path
import argparse, hashlib, json, re, sys

EXPECTED_MIGRATION_HASHES = {
    "MIGRATION_13_14": "e2f85ee75230d98abe9cb5a360d4b274089b26a465e877cd374cbfbf73a88052",
    "MIGRATION_14_15": "10ccb9ad1f41e15b15671254525adeebfc50a38ba14be94bcf9e47d9de34f1ad",
    "MIGRATION_15_16": "0dd0d0bf18b233d1fd4213ded840f981a3c7ee42a72d806682b5cb4b16df2d43",
    "MIGRATION_16_17": "6d096b57d58b399753a9e495d98765e21ae2e57f764a7e4bf422f2a086b56862",
    "MIGRATION_17_18": "9b03b4058ca6646db3009e8fddcb0a3f84b9a6ec2655c9b60c99940c27000cf1",
}

def sha(s): return hashlib.sha256(s.encode()).hexdigest()
def read(root, rel): return (root/rel).read_text()

def migration_hashes(db):
    out={}
    for label in EXPECTED_MIGRATION_HASHES:
        start=db.index(f"val {label}")
        m=re.search(r"^        \}$", db[start:], re.M)
        if not m: raise ValueError(f"cannot close {label}")
        out[label]=sha(db[start:start+m.end()].strip())
    return out

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('--root',default='.'); ap.add_argument('--output')
    a=ap.parse_args(); root=Path(a.root).resolve()
    checks=[]
    def check(name, cond, detail=""):
        checks.append({"name":name,"passed":bool(cond),"detail":detail if not cond else ""})

    db=read(root,'core/database/src/main/kotlin/com/autodrive/app/core/database/AutoDriveDatabase.kt')
    prod_roots=[root/'app/src/main', root/'core', root/'feature']
    prod_kotlin=[p for base in prod_roots if base.exists() for p in base.rglob('*.kt') if '/src/test/' not in str(p) and '/src/androidTest/' not in str(p)]
    entity=read(root,'core/database/src/main/kotlin/com/autodrive/app/core/database/entities/SyncObservabilityStateEntity.kt')
    dao=read(root,'core/database/src/main/kotlin/com/autodrive/app/core/database/dao/SyncObservabilityDao.kt')
    context=read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/diagnostics/SyncRunContext.kt')
    health=read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/diagnostics/SyncHealthSnapshot.kt')
    store=read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/diagnostics/SyncObservabilityStore.kt')
    diag=read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/diagnostics/SyncDiagnostics.kt')
    coord=read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/DefaultSyncCoordinator.kt')
    manager=read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/SyncManager.kt')
    realtime=read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/realtime/RealtimeManager.kt')
    redactor=read(root,'core/observability/src/main/kotlin/com/autodrive/app/core/observability/SensitiveDataRedactor.kt')
    fault=read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/fault/SyncFaultInjector.kt')
    faultmod=read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/di/SyncFaultModule.kt')
    cleaner=read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/LocalDataCleaner.kt')
    unified=read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/UnifiedChangeSynchronizer.kt')
    bootstrap=read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/SafeBootstrapSynchronizer.kt')
    anti=read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/AntiEntropyReconciler.kt')

    check('room_version_19', 'AUTODRIVE_DATABASE_VERSION = 19' in db)
    check('migration_18_19_exists', 'MIGRATION_18_19 = object : Migration(18, 19)' in db)
    check('observability_entity_registered', 'SyncObservabilityStateEntity::class' in db and 'syncObservabilityDao' in db)
    check('observability_exact_scope_pk', 'primaryKeys = ["user_id", "client_id", "org_id", "stream"]' in entity)
    required_cols=['last_sync_run_id','last_local_cursor_revision','last_server_head_revision','last_server_head_observed_at','last_successful_bootstrap_at','cursor_expiry_count','reconciliation_mismatch_count','hint_received_count','hint_trailing_run_count','hint_dropped_count','last_realtime_state']
    check('required_observability_columns', all(c in entity and c in db for c in required_cols), ','.join(c for c in required_cols if c not in entity or c not in db))
    check('logout_cleans_exact_scope_observability','syncObservabilityDao().deleteForScope(scope.userId, scope.clientId, scope.orgId)' in cleaner)
    check('historical_migrations_unchanged', migration_hashes(db)==EXPECTED_MIGRATION_HASHES, json.dumps(migration_hashes(db),sort_keys=True))
    check('no_destructive_migration','fallbackToDestructiveMigration' not in '\n'.join(p.read_text(errors='ignore') for p in prod_kotlin))
    check('sync_run_context_fields', all(x in context for x in ['syncRunId','requestedGeneration','scopeFingerprint','SecureRandom','SHA-256']))
    check('unique_run_id_generated','UUID.randomUUID().toString()' in coord and 'SyncRunContext(' in coord)
    check('correlation_propagates_to_engine','engine.synchronize(context)' in coord and 'override suspend fun synchronize(\n        context: SyncRunContext' in manager)
    check('health_snapshot_api', all(x in health for x in ['revisionLag','oldestOutboxAgeMs','retryCount','deadLetterCount','conflictCount','failedRealtimeParticipants','cursorExpiryCount','hintDroppedCount','freshness']))
    check('unknown_remote_head_truth','UNKNOWN_REMOTE_HEAD' in store and 'head != null' in store)
    check('outbox_metrics_scoped', all(x in read(root,'core/database/src/main/kotlin/com/autodrive/app/core/database/dao/PendingOperationDao.kt') for x in ['oldestActiveCreatedAt','sumActiveAttemptCount','user_id=:userId AND client_id=:clientId AND org_id=:orgId']))
    check('raw_scope_redaction', all(x in redactor for x in ['"user_id"','"client_id"','"org_id"','"email"','"authorization"']))
    check('scope_fingerprint_not_phone_redacted', '(?<![A-Za-z0-9])' in redactor and '(?![A-Za-z0-9])' in redactor)
    check('diagnostics_has_run_correlation', all(x in diag for x in ['"sync_run_id"','"scope_fingerprint"','"requested_generation"']))
    check('diagnostics_has_core_metrics', all(x in diag for x in ['local_cursor_revision','server_head_revision','revision_lag','oldest_outbox_age_ms','retry_count','conflict_count']))
    outbox_sync=read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/OutboxSynchronizer.kt')
    check('outbox_operation_correlation', all(x in outbox_sync for x in ['sync_run_id','scope_fingerprint','mutation_id','operation_type','failure_category','stable_error_code']))
    check('inbound_group_correlation', all(x in diag for x in ['first_event_id','last_event_id','first_revision','last_revision','entity_type','operation']) and 'diagnostics.changeGroup(' in unified)
    check('stable_failure_code_only', '[A-Z0-9_:-]{2,80}' in coord and '[A-Z0-9_:-]{2,80}' in store)
    check('realtime_observability_scope_guard', 'currentScope == targetScope' in realtime and 'SyncScope.from(sessionReader.currentSession()) == targetScope' in realtime)
    check('production_fault_binding_noop','provideSyncFaultInjector(): SyncFaultInjector = NoOpSyncFaultInjector()' in faultmod)
    check('no_production_fault_switch', not any(re.search(r'(ENABLE_FAULT|enableFault|fault.*RemoteConfig|remote.*fault)', p.read_text(errors='ignore'), re.I) for p in root.rglob('*.kt')))
    required_points=['OUTBOX_AFTER_LOCAL_COMMIT_BEFORE_SEND','OUTBOX_AFTER_SERVER_COMMIT_BEFORE_RESPONSE','OUTBOX_BEFORE_FINALIZE_LOCAL_RECEIPT','CHANGE_FEED_AFTER_FETCH_BEFORE_APPLY','CHANGE_GROUP_AFTER_ENTITY_APPLY_BEFORE_CURSOR_COMMIT','CHANGE_PAGE_AFTER_COMMIT_BEFORE_NEXT_FETCH','BOOTSTRAP_AFTER_BEGIN','BOOTSTRAP_AFTER_STAGE_PAGE_COMMIT','BOOTSTRAP_BEFORE_INSTALL_COMMIT','BOOTSTRAP_AFTER_INSTALL_BEFORE_DELTA_RESUME','RECONCILE_AFTER_MANIFEST','RECONCILE_AFTER_TARGETED_REPAIR_BEFORE_RECHECK','COORDINATOR_DURING_PUSH','COORDINATOR_DURING_PULL','LOGOUT_DURING_ACTIVE_SYNC','WORKER_AFTER_LEASE_CLAIM']
    check('fault_points_complete', all(x in fault for x in required_points))
    check('fault_hooks_wired', all(x in (unified+bootstrap+anti+manager+read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/outbox/PendingOperationProcessor.kt')) for x in ['CHANGE_GROUP_AFTER_ENTITY_APPLY_BEFORE_CURSOR_COMMIT','BOOTSTRAP_BEFORE_INSTALL_COMMIT','RECONCILE_AFTER_MANIFEST','COORDINATOR_DURING_PUSH','WORKER_AFTER_LEASE_CLAIM']))
    check('realtime_no_business_room_authority','AutoDriveDatabase' not in realtime and '.invoiceDao()' not in realtime and '.paymentDao()' not in realtime)
    check('push_before_canonical_pull', manager.index('outboxSynchronizer.flush(scope, recoverExpiredClaims = false, context = context)') < manager.index('unifiedChangeSynchronizer.synchronize(scope, context)'))
    check('recover_before_push', manager.index('outboxSynchronizer.recoverExpiredLeases(scope)') < manager.index('outboxSynchronizer.flush(scope, recoverExpiredClaims = false, context = context)'))
    check('generation_safety_preserved', all(x in coord for x in ['requestedGeneration += 1','completedGeneration = maxOf(completedGeneration, generationToService)','requestedGeneration <= completedGeneration']))
    check('legacy_incremental_authority_zero','LegacyRemotePuller' not in manager)
    check('separate_deletion_authority_zero','DeletionSynchronizer' not in manager)
    check('v72_authorities_still_wired', all(x in manager for x in ['unifiedChangeSynchronizer','safeBootstrapSynchronizer','antiEntropyReconciler']))
    check('canonical_stream_preserved','autodrive-global-change-v1' in read(root,'core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/UnifiedSyncProtocol.kt'))
    check('observability_not_correctness_authority', all('SyncObservabilityStore' not in read(root,p) for p in ['core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/ChangeEventApplier.kt','core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/PendingLocalMutationGuard.kt']))
    raw_offenders=[]
    for p in root.rglob('*.kt'):
        if p.name in {'AppLogger.kt','SmsHashLogger.kt'}: continue
        if re.search(r'\bLog\.(d|i|w|e|v)\(',p.read_text(errors='ignore')): raw_offenders.append(str(p.relative_to(root)))
    check('raw_android_log_policy', not raw_offenders, ','.join(raw_offenders))
    check('no_v73_waiver', not any('V73_WAIVER' in p.read_text(errors='ignore') for p in prod_kotlin))

    result={"session":73,"verifier":"static","totalCount":len(checks),"passedCount":sum(c['passed'] for c in checks),"passed":all(c['passed'] for c in checks),"assertions":checks}
    text=json.dumps(result,indent=2,sort_keys=True)+"\n"
    if a.output: Path(a.output).write_text(text)
    else: print(text,end='')
    sys.exit(0 if result['passed'] else 1)
if __name__=='__main__': main()
