package com.autodrive.app.architecture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSyncV71ArchitectureTest {

    @Test
    fun chatRecovery_usesOneServerOwnedAdvancingCursorAndAtomicCheckpoint() {
        val recovery = ProjectLayout.source("core/sync/data/ChatRecoverySynchronizer.kt").readText()
        val legacy = ProjectLayout.source("core/sync/data/LegacyRemotePuller.kt").readText()
        val repository = ProjectLayout.source("feature/chat/data/ChatRepositoryImpl.kt").readText()
        val server = ProjectLayout.projectRoot.resolve("supabase/migrations/20260821224500_autodrive_chat_recovery_commands_v1.sql").readText()

        assertTrue(recovery.contains("p_after_seq"))
        assertTrue(recovery.contains("chatRecoverySequence"))
        assertTrue(recovery.contains("chatRecoveryCheckpointDao().upsert"))
        assertTrue(legacy.contains("chatRecovery.recover(scope)"))
        assertTrue(repository.contains("chatRecovery.recover(scope)"))
        assertFalse((legacy + repository + recovery).contains("limit(100)"))
        assertTrue(server.contains("new.chat_recovery_seq := nextval"))
        assertTrue(server.contains("m.chat_recovery_seq > p_after_seq"))
        assertTrue(server.contains("CHAT_RECOVERY_IDENTITY_IMMUTABLE"))
    }

    @Test
    fun mediaSend_isDurableBeforeNetworkAndUsesStableObjectIdentity() {
        val repository = ProjectLayout.source("feature/chat/data/ChatRepositoryImpl.kt").readText()
        val media = ProjectLayout.source("feature/chat/data/ChatMediaManager.kt").readText()
        val processor = ProjectLayout.source("feature/chat/data/ChatMediaTransferProcessor.kt").readText()
        val contracts = ProjectLayout.source("core/sync/outbox/OutboxContracts.kt").readText()

        assertTrue(repository.contains("db.chatMediaTransferDao().insert"))
        assertTrue(repository.contains("db.pendingOperationDao().insert"))
        assertTrue(media.contains("messageId-${'$'}{contentSha256.take(24)}"))
        assertTrue(processor.contains("updatePayloadBeforeFirstAttempt"))
        assertTrue(processor.contains("mediaObjectPath = uploaded.objectPath"))
        assertTrue(contracts.contains("media_object_path"))
    }

    @Test
    fun conversationCreate_isDurableIdempotentAndOrdersChildSends() {
        val repository = ProjectLayout.source("feature/chat/data/ChatRepositoryImpl.kt").readText()
        val outbox = ProjectLayout.source("core/sync/data/OutboxSynchronizer.kt").readText()
        val dao = ProjectLayout.source("core/database/dao/PendingOperationDao.kt").readText()
        val server = ProjectLayout.projectRoot.resolve("supabase/migrations/20260821224500_autodrive_chat_recovery_commands_v1.sql").readText()

        assertFalse(repository.contains("rpc(\"create_new_conversation\""))
        assertTrue(repository.contains("CREATE_CHAT_CONVERSATION"))
        assertTrue(repository.contains("dependsOnMutationId = createParent?.mutationId"))
        assertTrue(dao.contains("depends_on_mutation_id IS NULL OR NOT EXISTS"))
        assertTrue(outbox.contains("getChildrenByDependency"))
        assertTrue(server.contains("autodrive_command_existing_or_conflict_v1"))
        assertTrue(server.contains("public.create_new_conversation(v_scope.client_id"))
    }
}
