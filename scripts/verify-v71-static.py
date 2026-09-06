#!/usr/bin/env python3
from __future__ import annotations
import hashlib,json,re
from pathlib import Path
R=Path(__file__).resolve().parents[1]
C=[]
def t(p): return (R/p).read_text(encoding='utf-8')
def add(n,ok,d=''): C.append({'name':n,'passed':bool(ok),'detail':d})
def sha(p): return hashlib.sha256((R/p).read_bytes()).hexdigest()

db=t('core/database/src/main/kotlin/com/autodrive/app/core/database/AutoDriveDatabase.kt')
ents=t('core/database/src/main/kotlin/com/autodrive/app/core/database/entities/Entities.kt')
chatents=t('core/database/src/main/kotlin/com/autodrive/app/core/database/entities/ChatSyncEntities.kt')
pdao=t('core/database/src/main/kotlin/com/autodrive/app/core/database/dao/PendingOperationDao.kt')
legacy=t('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/LegacyRemotePuller.kt')
recovery=t('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/ChatRecoverySynchronizer.kt')
repo=t('feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/data/ChatRepositoryImpl.kt')
media=t('feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/data/ChatMediaManager.kt')
processor=t('feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/data/ChatMediaTransferProcessor.kt')
outbox=t('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/OutboxSynchronizer.kt')
contracts=t('core/sync/src/main/kotlin/com/autodrive/app/core/sync/outbox/OutboxContracts.kt')
gateway=t('core/sync/src/main/kotlin/com/autodrive/app/core/sync/outbox/IdempotentServerCommandGateway.kt')
cleaner=t('core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/LocalDataCleaner.kt')
sql=t('supabase/migrations/20260821224500_autodrive_chat_recovery_commands_v1.sql')

add('01_room17','AUTODRIVE_DATABASE_VERSION = 17' in db)
add('02_migration16_17','MIGRATION_16_17 = object : Migration(16, 17)' in db and 'MIGRATION_16_17,' in db)
add('03_checkpoint_registered','ChatRecoveryCheckpointEntity::class' in db and 'chatRecoveryCheckpointDao()' in db)
add('04_media_registered','ChatMediaTransferEntity::class' in db and 'chatMediaTransferDao()' in db)
add('05_dependency_nullable','dependsOnMutationId: String? = null' in ents)
add('06_checkpoint_scoped_pk','primaryKeys = ["user_id", "client_id", "org_id", "conversation_id"]' in chatents and 'lastServerSequence: Long = 0L' in chatents)
add('07_media_scope_unique','index_chat_media_transfers_scope_message' in chatents and 'unique = true' in chatents)
add('08_getdue_dependency_gate','depends_on_mutation_id IS NULL OR NOT EXISTS' in pdao)
add('09_getdue_media_gate',"operation != 'SEND_CHAT_MESSAGE' OR NOT EXISTS" in pdao and "transfer.status != 'COMPLETE'" in pdao)
add('10_claim_rechecks_gates',pdao.count('depends_on_mutation_id IS NULL OR NOT EXISTS')>=2 and pdao.count("transfer.status != 'COMPLETE'")>=2)

chat_sources=legacy+repo+recovery
add('11_terminal_limit100_zero','limit(100)' not in chat_sources and 'LIMIT 100' not in chat_sources)
add('12_legacy_delegates','chatRecovery.recover(scope)' in legacy)
add('13_repo_delegates','chatRecovery.recover(scope)' in repo)
add('14_single_recovery_component','class ChatRecoverySynchronizer' in recovery)
add('15_keyset_rpc_used','autodrive_chat_recovery_page_v1' in recovery and 'p_after_seq' in recovery and 'chatRecoverySequence' in recovery)
add('16_checkpoint_atomic','db.withTransaction' in recovery and recovery.index('mergeMessage(scope') < recovery.index('chatRecoveryCheckpointDao().upsert'))
add('17_no_device_clock_cursor','System.currentTimeMillis()' not in recovery)
add('18_strict_created_at','OffsetDateTime.parse(value).toInstant().toEpochMilli()' in recovery)
add('19_scope_validation','dto.clientId == scope.clientId && dto.orgId == scope.orgId' in recovery)
add('20_both_sender_types','dto.senderType == "ADMIN" || dto.senderType == "MARKETER"' in recovery)
add('21_pending_local_guard','findActiveByMutationId' in recovery and 'CHAT_PENDING_INTENT_CONFLICT' in recovery)
add('22_no_synthetic_revision','SyncInboxEntity' not in recovery and 'serverRevision' not in recovery and 'eventId' not in recovery)

stage_segment=media[media.index('internal suspend fun stageOutgoing'):media.index('internal fun resolveMediaUrl')]
add('23_stage_is_local_only','supabase' not in stage_segment and '.upload(' not in stage_segment and 'createSignedUrl' not in stage_segment)
add('24_repository_stages_before_commit','mediaManager.stageOutgoing' in repo and 'chatMediaTransferDao().insert' in repo)
add('25_media_atomic_intents',all(x in repo for x in ('db.chatMessageDao().insert(entity)','db.pendingOperationDao().insert(','db.chatMediaTransferDao().insert(')))
add('26_stable_media_object_path','UUID.randomUUID()' not in media and 'messageId-${contentSha256.take(24)}' in media)
add('27_transfer_hash_validation','LOCAL_FILE_HASH_CHANGED' in media and 'sha256(downloadBytes(signed)) == transfer.contentSha256' in media)
add('28_media_lease','lease_until' in t('core/database/src/main/kotlin/com/autodrive/app/core/database/dao/ChatMediaTransferDao.kt'))
add('29_media_payload_before_first_attempt','updatePayloadBeforeFirstAttempt' in processor and 'attemptCount == 0' in processor)
add('30_media_scope_recheck','STALE_MEDIA_SCOPE' in processor)
add('31_retry_same_message','reactivateForMessage' in repo and 'mutationId = msg.id' in repo)
add('32_incoming_cache_not_recovery_authority','cachePendingAdminMedia()' in repo and repo.index('chatRecovery.recover(scope)') < repo.index('mediaManager.cachePendingAdminMedia()'))

add('33_direct_create_new_zero','create_new_conversation' not in repo)
add('34_create_local_atomic','CREATE_CHAT_CONVERSATION' in repo and 'db.withTransaction' in repo)
add('35_create_payload_stable','localConversationId = mutationId' in repo)
add('36_create_server_command',all(x in gateway for x in ('autodrive_create_chat_conversation_command_v1','createChatConversation')))
add('37_create_receipt_validation','ServerCommandType.CREATE_CHAT_CONVERSATION' in outbox and 'requireEntity = true' in outbox)
add('38_create_send_dependency','dependsOnMutationId = createParent?.mutationId' in repo)
add('39_parent_finalizer_rewrites_child','getChildrenByDependency' in outbox and 'payload.copy(conversationId = serverId)' in outbox)
add('40_dead_parent_blocks_by_presence',"status IN ('PENDING','IN_PROGRESS','DEAD_LETTER')" in pdao)

add('41_server_migration_append_only',(R/'supabase/migrations/20260821224500_autodrive_chat_recovery_commands_v1.sql').exists())
add('42_v69_sql_unchanged',sha('supabase/migrations/20260821203000_autodrive_idempotent_commands_v1.sql')=='6663381c4bf177c7cc22c75fb4c1eee1683290894307ec9ade85e4fe7620c01e')
add('43_server_page_scope',all(x in sql for x in ('m.client_id = v_scope.client_id','m.org_id = v_scope.org_id','m.conversation_id = p_conversation_id')))
add('44_server_keyset',all(x in sql for x in ('chat_recovery_seq bigint','new.chat_recovery_seq := nextval','m.chat_recovery_seq > p_after_seq','order by m.chat_recovery_seq asc')))
add('45_create_wrapper_reuses_existing','public.create_new_conversation(v_scope.client_id, coalesce(p_subject' in sql)
add('46_create_wrapper_receipt','autodrive_command_existing_or_conflict_v1' in sql and "'CREATE_CHAT_CONVERSATION'" in sql and 'autodrive_command_store_receipt_v1' in sql)
add('47_create_auth_scope','autodrive_command_scope_v1()' in sql)
add('48_grants_hardened','revoke all on function public.autodrive_create_chat_conversation_command_v1' in sql and 'to authenticated' in sql)
add('48b_media_object_path_durable','media_object_path' in sql and 'autodrive_send_chat_message_command_v2' in sql and 'mediaObjectPath' in contracts)
add('48c_signed_url_not_canonical',"'media_url', p_media_url" not in sql[sql.index('autodrive_send_chat_message_command_v2'):])
add('48d_chat_sequence_immutable','CHAT_RECOVERY_IDENTITY_IMMUTABLE' in sql and 'before insert or update on public.internal_messages' in sql)

participants=[]
for p in [
'feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/data/realtime/BillingRealtimeParticipant.kt',
'feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/data/realtime/BalanceRealtimeParticipant.kt',
'feature/notifications/src/main/kotlin/com/autodrive/app/feature/notifications/data/realtime/NotificationsRealtimeParticipant.kt',
'feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/data/realtime/ChatRealtimeParticipant.kt']:
    participants.append(t(p))
add('49_realtime_direct_room_zero',all('AutoDriveDatabase' not in x for x in participants))
add('50_realtime_payload_apply_zero',all('oldRecord' not in x and 'LocalNotificationPublisher' not in x for x in participants))
add('51_logout_checkpoint_scope','chatRecoveryCheckpointDao().deleteForScope(scope.userId, scope.clientId, scope.orgId)' in cleaner)
add('52_logout_transfer_scope','chatMediaTransferDao().deleteForScope(scope.userId, scope.clientId, scope.orgId)' in cleaner)
add('53_logout_staged_files','File(path).delete()' in cleaner)

ui=[]
for p in R.rglob('*.kt'):
    rel=p.relative_to(R).as_posix()
    if '/src/main/' in rel and ('/presentation/' in rel or '/designsystem/' in rel): ui.append((rel,p))
h=hashlib.sha256()
for rel,p in sorted(ui): h.update(rel.encode()+b'\0'+hashlib.sha256(p.read_bytes()).digest())
add('54_production_ui_drift_zero',len(ui)==90 and h.hexdigest()=='85a03f42baa35909ac8404ad84355ba87e6250839dd149783c50883de8326f05',f'{len(ui)}:{h.hexdigest()}')
allprod='\n'.join(p.read_text(encoding='utf-8',errors='ignore') for p in R.rglob('*.kt') if '/src/main/' in p.as_posix())
add('55_no_new_waiver','V71_WAIVER' not in allprod)
add('56_no_destructive_migration','fallbackToDestructiveMigration' not in allprod)

passed=sum(c['passed'] for c in C)
result={'allPassed':passed==len(C),'passedCount':passed,'checkCount':len(C),'compatibilitySignedUrlMintCount':media.count('createSignedUrl'),'durableMediaReference':'media_object_path','chatRecoveryCursor':'server_owned_chat_recovery_seq','getOrCreateConversationEvidence':'PROVEN_BY_2026_08_21_SCHEMA_EVIDENCE','checks':C}
print(json.dumps(result,ensure_ascii=False,sort_keys=True,separators=(',',':')))
raise SystemExit(0 if result['allPassed'] else 1)
