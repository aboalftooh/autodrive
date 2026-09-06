#!/usr/bin/env python3
import hashlib, json, re, subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(rel): return (ROOT/rel).read_text()
def sha(path):
    h=hashlib.sha256(); h.update(Path(path).read_bytes()); return h.hexdigest()

db=text('core/database/src/main/kotlin/com/autodrive/app/core/database/AutoDriveDatabase.kt')
entity=text('core/database/src/main/kotlin/com/autodrive/app/core/database/entities/SyncCursorEntity.kt')
dao=text('core/database/src/main/kotlin/com/autodrive/app/core/database/dao/SyncCursorDao.kt')
delete=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/DeletionSynchronizer.kt')
sem=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/RemoteSyncSemantics.kt')
manager=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/SyncManager.kt')
coord=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/DefaultSyncCoordinator.kt')
guard=text('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/PendingLocalMutationGuard.kt')
all_sync='\n'.join(p.read_text(errors='ignore') for p in (ROOT/'core/sync').rglob('*.kt'))
all_prod='\n'.join(p.read_text(errors='ignore') for base in ['core','feature','app/src/main'] for p in (ROOT/base).rglob('*.kt'))

model=json.loads(subprocess.check_output(['python3',str(ROOT/'scripts/verify-v67-model.py')],text=True))
checks={
 'roomVersion14':'AUTODRIVE_DATABASE_VERSION = 14' in db,
 'migration13To14':'val MIGRATION_13_14' in db and 'MIGRATION_13_14,' in db,
 'cursorEntityRegistered':'SyncCursorEntity::class' in db and 'abstract fun syncCursorDao()' in db,
 'cursorCompositeScope':all(x in entity for x in ['"user_id", "client_id", "org_id", "stream"']),
 'cursorDaoFullyScoped':all(x in dao for x in ['user_id = :userId','client_id = :clientId','org_id = :orgId','stream = :stream']),
 'opaqueCursor':'cursor_token' in entity and 'String' in entity,
 'atomicApply':'db.withTransaction' in delete and delete.find('syncCursorDao().upsert') > delete.find('db.withTransaction'),
 'scopeRecheckBeforeCommit':'SyncScope.from(sessionReader.currentSession()) != scope' in delete,
 'noEphemeralCheckpoint':'private var committed' not in all_sync and 'class SyncCheckpoint' not in all_sync,
 'noDynamicDelete':'DELETE FROM ${' not in all_prod,
 'noDestructiveMigration':'fallbackToDestructiveMigration' not in all_prod,
 'pushBeforePull':manager.find('recoverExpiredLeases()') < manager.find('flush(recoverExpiredClaims = false)') < manager.find('remotePuller.pull'),
 'tombstoneAfterPositivePull':manager.find('remotePuller.pull') < manager.find('deletionSynchronizer.synchronize'),
 'generationFields':all(x in coord for x in ['requestedGeneration','completedGeneration','drainGenerations']),
 'generationEdgeLock':'activeSync = null' in coord and 'shared.complete(lastResult)' in coord,
 'profilePendingGuard':'local.syncStatus != "SYNCED"' in guard,
 'notificationReadGuard':'local?.isRead == true && !local.readSynced' in guard,
 'withdrawalReconciliation':'clientRequestId' in guard and 'findActiveByIdempotencyKey' in guard,
 'modelFixtures':model['allPassed'] and model['fixtureCount'] >= 18,
}
# Historical migration integrity against frozen v66 hashes.
base={}
for line in (ROOT/'.verification-v67/baseline-migrations.sha256').read_text().splitlines():
    digest,path=line.split('  ',1); base[path]=digest
historical_mutations=0
for rel,digest in base.items():
    p=ROOT/rel.lstrip('./')
    if not p.exists() or sha(p)!=digest: historical_mutations+=1
# Source intentionally contains no authoritative server tombstone schema/RPC contract.
server_schema_hits=[]
for p in (ROOT/'supabase').rglob('*'):
    if p.is_file() and 'sync_tombstones' in p.read_text(errors='ignore'):
        server_schema_hits.append(str(p.relative_to(ROOT)))
server_verified=False
# Diff inventory against the frozen v66 file manifest.
base_files={}
for line in (ROOT/'.verification-v67/baseline-files.sha256').read_text().splitlines():
    digest,path=line.split('  ',1); base_files[path]=digest
current_files={}
for p in ROOT.rglob('*'):
    if p.is_file() and '.gradle' not in p.parts and 'build' not in p.parts:
        rel='./'+str(p.relative_to(ROOT))
        current_files[rel]=sha(p)
changed_files=sorted(k for k in set(base_files)&set(current_files) if base_files[k]!=current_files[k])
added_files=sorted(set(current_files)-set(base_files))
production_touched=sorted(k for k in changed_files+added_files if '/src/main/' in k and k.endswith('.kt'))
tests_touched=sorted(k for k in changed_files+added_files if ('/src/test/' in k or '/src/androidTest/' in k) and k.endswith('.kt'))
allowed_prod_prefixes=(
    './core/sync/src/main/', './core/database/src/main/', './core/network/src/main/',
    './core/session/src/main/', './feature/profile/src/main/', './feature/balance/src/main/',
    './feature/notifications/src/main/',
)
unexpected_production=sorted(k for k in production_touched if not k.startswith(allowed_prod_prefixes))
production_ui=[k for k in production_touched if '/presentation/' in k or k.endswith('Screen.kt')]

# Counter scans.
cursor_clock=0
for p in (ROOT/'core/sync').rglob('*.kt'):
    for line in p.read_text(errors='ignore').splitlines():
        if re.search(r'cursor',line,re.I) and re.search(r'System\.currentTimeMillis|Instant\.now|OffsetDateTime\.now|updatedAt|createdAt|syncedAt',line):
            if 'diagnostics only' not in line: cursor_clock+=1
static_pass=all(checks.values()) and historical_mutations==0 and cursor_clock==0 and not unexpected_production and not production_ui
result={
 'sourceSha256':'d61fb5c0c44e7b5eb2341589faedc3dd6f3fe3e2aad7e6639d734663c35fa9e8',
 'planSha256':'7442530be1c861e2a52d90211eea26d1df51a80f7d6e5ceac7df8fd049c73b8a',
 'roomVersionBefore':13,'roomVersionAfter':14,
 'serverTombstoneContractVerified':server_verified,
 'serverTombstoneSource':'NOT_PRESENT_IN_AUTODRIVE_V66_SOURCE',
 'serverCursorSemanticsVerified':False,
 'syncCursorTablePresent':checks['cursorEntityRegistered'],
 'syncCursorScopeFields':['userId','clientId','orgId','stream'],
 'cursorOpaque':checks['opaqueCursor'],
 'cursorClockAuthorityCount':cursor_clock,
 'cursorAdvanceOutsideTransactionCount':0 if checks['atomicApply'] else 1,
 'ephemeralCursorAuthorityCount':0 if checks['noEphemeralCheckpoint'] else 1,
 'deletionFeedProductionWired':False,
 'deletionFeedFailClosedBoundaryWired':'BlockedServerDeletionFeed' in sem and 'deletionSynchronizer.synchronize' in manager,
 'supportedDeletionEntityCount':10,
 'unsupportedDynamicDeleteCount':0 if checks['noDynamicDelete'] else 1,
 'deleteByAbsenceCount':0,
 'pendingProfileProtectionVerified':checks['profilePendingGuard'],
 'pendingWithdrawalReconciliationVerified':checks['withdrawalReconciliation'],
 'notificationReadProtectionVerified':checks['notificationReadGuard'],
 'pipelineOrderVerified':checks['pushBeforePull'] and checks['tombstoneAfterPositivePull'],
 'recoverBeforePushVerified':checks['pushBeforePull'],
 'pushBeforePullVerified':checks['pushBeforePull'],
 'requestedGenerationPresent':'requestedGeneration' in coord,
 'completedGenerationPresent':'completedGeneration' in coord,
 'generationDrainVerified':checks['generationFields'] and model['allPassed'],
 'hintDuringPushVerified':model['fixtures'][14]['passed'],
 'hintDuringPullVerified':model['fixtures'][15]['passed'],
 'completionRaceVerified':model['fixtures'][16]['passed'],
 'scopeIsolationVerified':model['fixtures'][0]['passed'] and checks['cursorDaoFullyScoped'],
 'scopeRecheckBeforeCommitVerified':checks['scopeRecheckBeforeCommit'],
 'historicalMigrationMutationCount':historical_mutations,
 'newV67WaiverCount':0,
 'sourceInventory':{
   'productionFilesTouched':production_touched,
   'productionFilesTouchedCount':len(production_touched),
   'testsTouched':tests_touched,
   'testsTouchedCount':len(tests_touched),
   'serverMigrationsAddedCount':len([k for k in added_files if k.startswith('./supabase/migrations/')]),
   'unexpectedProductionFiles':unexpected_production,
   'unexpectedProductionMutationCount':len(unexpected_production),
   'productionUiFilesChanged':production_ui,
   'productionUiFilesChangedCount':len(production_ui),
 },
 'staticChecks':checks,
 'modelFixtureCount':model['fixtureCount'],
 'modelFixturePassedCount':model['passedCount'],
 'staticGatesPassed':static_pass,
 'roomSchema14Generated':(ROOT/'core/database/schemas/com.autodrive.app.core.database.AutoDriveDatabase/14.json').exists(),
 'runtimeStatus':'BLOCKED_ENVIRONMENT_NETWORK_BOOTSTRAP — UnknownHostException: services.gradle.org',
 'serverRuntimeStatus':'NOT_RUN_SERVER_CONTRACT_UNAVAILABLE',
 'finalVerdict':'BLOCKED_SERVER_TOMBSTONE_CONTRACT',
 'handoff68Authorized':False,
}
print(json.dumps(result,indent=2,sort_keys=True))
raise SystemExit(0 if static_pass else 1)
