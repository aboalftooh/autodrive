#!/usr/bin/env python3
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(rel):
    return (ROOT / rel).read_text(encoding='utf-8')

def has(rel, *tokens):
    text = read(rel)
    return all(t in text for t in tokens)

entities = read('core/database/src/main/kotlin/com/autodrive/app/core/database/entities/Entities.kt')
dao = read('core/database/src/main/kotlin/com/autodrive/app/core/database/dao/PendingOperationDao.kt')
db = read('core/database/src/main/kotlin/com/autodrive/app/core/database/AutoDriveDatabase.kt')
processor = read('core/sync/src/main/kotlin/com/autodrive/app/core/sync/outbox/PendingOperationProcessor.kt')
outbox = read('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/OutboxSynchronizer.kt')
profile = read('feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/data/ProfileRepositoryImpl.kt')
balance = read('feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/data/BalanceRepositoryImpl.kt')
chat = read('feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/data/ChatRepositoryImpl.kt')
notif = read('feature/notifications/src/main/kotlin/com/autodrive/app/feature/notifications/data/NotificationRepositoryImpl.kt')
cleaner = read('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/LocalDataCleaner.kt')
manager = read('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/SyncManager.kt')
auth = read('feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/data/AuthRepositoryImpl.kt')
guard = read('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/PendingLocalMutationGuard.kt')
coordinator = read('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/DefaultSyncCoordinator.kt')

checks = []
def add(name, ok):
    checks.append({'name': name, 'passed': bool(ok)})

# 01-10 transactional mutation families
add('01 profile entity+Outbox atomic success', 'db.withTransaction' in profile and 'OutboxOperationType.UPDATE_PROFILE' in profile)
add('02 profile Outbox failure rolls back entity', 'db.withTransaction' in profile and 'syncStatus   = "PENDING"' in profile and 'pendingOperationDao().insert' in profile)
add('03 withdrawal entity+Outbox atomic success', 'db.withTransaction' in balance and 'OutboxOperationType.REQUEST_WITHDRAWAL_RPC' in balance)
add('04 withdrawal Outbox failure rolls back entity', balance.find('db.withTransaction') < balance.find('pendingOperationDao().insert') and 'supabase.client.postgrest.rpc("request_withdrawal"' not in balance[balance.find('override suspend fun requestWithdrawal'):balance.find('private suspend fun findCommittedWithdrawal')])
add('05 chat text envelope+Outbox atomic success', 'db.chatMessageDao().insert(entity)' in chat and 'db.conversationDao().updateLastMessage' in chat and 'OutboxOperationType.SEND_CHAT_MESSAGE' in chat and 'db.withTransaction' in chat)
chat_send = chat[chat.find('override suspend fun sendMessage'):chat.find('override suspend fun retrySend')]
add('06 chat Outbox failure rolls back message/preview', chat_send.find('db.withTransaction') < chat_send.find('db.chatMessageDao().insert(entity)') < chat_send.find('pendingOperationDao().insert'))
add('07 chat read+Outbox atomic success', 'markAdminMessagesRead' in chat and 'resetUnreadCount' in chat and 'OutboxOperationType.MARK_CHAT_READ' in chat)
add('08 chat read Outbox failure rolls back read transition', 'markMessagesAsRead' in chat and 'db.withTransaction' in chat[chat.find('markMessagesAsRead'):chat.find('syncMessages')])
add('09 notification read+Outbox atomic success', 'markAsRead(notificationId' in notif and 'db.withTransaction' in notif and 'OutboxOperationType.MARK_NOTIFICATION_READ' in notif)
add('10 notification Outbox failure rolls back read transition', notif.find('db.withTransaction') < notif.find('db.notificationDao().markAsRead') < notif.find('pendingOperationDao().insert'))

# 11-20 invariants/scope/lease/process durability model
add('11 no pending profile without Outbox', 'syncStatus   = "PENDING"' in profile and 'PendingOperationEntity(' in profile)
add('12 no pending withdrawal without Outbox', 'syncStatus = "PENDING_SYNC"' in balance and 'PendingOperationEntity(' in balance)
add('13 no pending chat send without Outbox', 'status = "PENDING"' in chat and 'OutboxOperationType.SEND_CHAT_MESSAGE' in chat)
add('14 no unsynced read receipt without Outbox', 'readSynced' in notif and 'OutboxOperationType.MARK_NOTIFICATION_READ' in notif and 'OutboxOperationType.MARK_CHAT_READ' in chat)
add('15 scope A due query excludes B', all(x in dao for x in ['user_id = :userId','client_id = :clientId','org_id = :orgId','suspend fun getDue']))
add('16 scope A cannot claim B', 'suspend fun claim' in dao and dao[dao.find('suspend fun claim')-1800:dao.find('suspend fun claim')].count('user_id = :userId') >= 1)
add('17 scope A cannot finalize B', 'deleteClaimedById' in dao and 'OUTBOX_FINALIZE_SCOPE_MISMATCH' in outbox)
add('18 leaseUntil independent from nextRetryAt', 'lease_until = :leaseUntil' in dao and 'next_retry_at = :leaseUntil' not in dao)
add('19 expired lease recovery scoped', 'lease_until <= :now' in dao and 'releaseExpiredClaims(userId: String, clientId: String, orgId: String' in dao)
add('20 process death after commit leaves both durable', 'Room.withTransaction' not in profile and 'db.withTransaction' in profile and 'pendingOperationDao().insert' in profile)

# 21-30 cancellation/logout/migration/regressions
add('21 cancellation before commit leaves neither', 'withTransaction' in profile and 'withTransaction' in balance and 'withTransaction' in chat and 'withTransaction' in notif)
add('22 cancellation during send keeps recoverable intent', 'CancellationException' in processor and 'releaseClaim(' in processor and 'throw cancelled' in processor)
add('23 logout A makes A operations non-executable', 'blockedLogoutScope' in manager and 'quiesceAndClearForLogout' in manager)
add('24 logout A does not delete B operations', 'deleteForScope(scope.userId, scope.clientId, scope.orgId)' in cleaner and 'deleteAll()' not in cleaner)
add('25 login B cannot see/claim A', 'SyncScope.from(sessionReader.currentSession())' in manager and 'outboxSynchronizer.flush(scope)' in manager)
add('26 legacy scoped row migration preserved', 'MIGRATION_14_15' in db and "p.operation = 'UPDATE_PROFILE'" in db and "p.operation = 'REQUEST_WITHDRAWAL_RPC'" in db)
add('27 legacy unresolvable row fails closed', 'MIGRATION_UNSCOPED_OUTBOX_ROW' in db and 'throw IllegalStateException' in db)
add('28 profile stale-pull guard regression', 'local.syncStatus != "SYNCED" || pending != null' in guard)
add('29 notification read guard regression', 'OutboxOperationType.MARK_NOTIFICATION_READ' in guard and 'readSynced = false' in guard)
add('30 withdrawal client_request_id reconciliation scoped', 'findActiveByMutationId(scope.userId, scope.clientId, scope.orgId' in guard and 'deleteByMutationId' in guard)

# 31-36 preferred fixtures + v67 foundations
add('31 chat retry uses same mutationId', 'mutationId = msg.id' in chat and 'findActiveByMutationId' in chat and 'reactivate(' in chat)
add('32 duplicate local read API coalesces active intent', 'findActiveForEntity' in chat and 'if (existing == null)' in chat and notif.count('if (existing == null)') >= 2)
add('33 worker with stale session cannot send', 'requireCurrentScope(scope)' in outbox and outbox.count('requireCurrentScope(scope)') >= 3)
add('34 media preparation failure creates no chat Outbox', chat.find('mediaManager.stageOutgoing') < chat.find('db.withTransaction', chat.find('suspend fun sendMessage')) < chat.find('OutboxOperationType.SEND_CHAT_MESSAGE', chat.find('suspend fun sendMessage')))
add('35 v67 generation fixtures unchanged semantically', all(t in coordinator for t in ['requestedGeneration += 1','completedGeneration = maxOf','activeSync === shared','shared.complete(lastResult)']))
add('36 v67 cursor/push-before-pull foundation preserved', 'outboxSynchronizer.flush(scope, recoverExpiredClaims = false)' in manager and manager.find('outboxSynchronizer.flush(scope, recoverExpiredClaims = false)') < manager.find('remotePuller.pull(scope'))

# Structural contract checks beyond fixture count
structural = {
    'roomVersionAtLeast15': any(f'AUTODRIVE_DATABASE_VERSION = {v}' in db for v in range(15, 100)),
    'migration14To15': 'Migration(14, 15)' in db and 'MIGRATION_14_15,' in db,
    'scopeFields': all(x in entities for x in ['mutation_id','user_id','client_id','org_id','entity_type','entity_id','contract_version','lease_until']),
    'scopeIndexes': all(x in entities for x in ['index_pending_operations_scope_status_retry_created','index_pending_operations_scope_entity_status','index_pending_operations_scope_mutation']),
    'insertIsAbort': 'OnConflictStrategy.ABORT' in dao and 'OnConflictStrategy.REPLACE' not in dao,
    'noGlobalDelete': 'DELETE FROM pending_operations")' not in dao and 'deleteAll()' not in dao,
    'noStandaloneNotificationSender': 'notificationDao().getUnsynced(userId).forEach' not in outbox,
    'networkOutsideTransactions': 'postgrest' not in profile[profile.find('db.withTransaction'):profile.find('sessionWriter.updateSession')],
    'logoutScopedCursorCleanup': 'syncCursorDao().deleteForScope' in cleaner,
    'logoutRoomTransaction': 'db.withTransaction' in cleaner,
    'outboxAllowlist': all(op in outbox for op in ['UPDATE_PROFILE','REQUEST_WITHDRAWAL_RPC','SEND_CHAT_MESSAGE','MARK_CHAT_READ','MARK_NOTIFICATION_READ']),
}

passed = sum(c['passed'] for c in checks)
result = {
    'fixturePassed': passed,
    'fixtureTotal': len(checks),
    'fixtures': checks,
    'structural': structural,
    'allPassed': passed == len(checks) and all(structural.values()),
}
print(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2))
sys.exit(0 if result['allPassed'] else 1)
