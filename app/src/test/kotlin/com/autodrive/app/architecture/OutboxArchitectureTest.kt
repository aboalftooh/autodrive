package com.autodrive.app.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxArchitectureTest {

    private val projectRoot = locateProjectRoot()

    @Test
    fun pendingOperations_haveBoundedRetryAndDeadLetterState() {
        val entity = source("core/database/entities/Entities.kt")
        val dao = source("core/database/dao/PendingOperationDao.kt")
        val policy = source("core/sync/outbox/OutboxRetryPolicy.kt")

        assertTrue(entity.contains("attempt_count"))
        assertTrue(entity.contains("next_retry_at"))
        assertTrue(entity.contains("last_error_code"))
        assertTrue(entity.contains("mutation_id"))
        assertTrue(entity.contains("user_id"))
        assertTrue(entity.contains("client_id"))
        assertTrue(entity.contains("org_id"))
        assertTrue(entity.contains("lease_until"))
        assertTrue(entity.contains("contract_version"))
        assertTrue(dao.contains("DEAD_LETTER"))
        assertTrue(policy.contains("OUTBOX_MAX_ATTEMPTS"))
        assertFalse(dao.contains("incrementRetry"))
        assertFalse(dao.contains("getAll()"))
    }

    @Test
    fun roomMigration_14To15_isConnected() {
        val database = source("core/database/AutoDriveDatabase.kt")

        assertTrue(database.contains("AUTODRIVE_DATABASE_VERSION = 17"))
        assertTrue(database.contains("MIGRATION_14_15"))
        assertTrue(database.contains("MIGRATION_13_14,"))
        assertTrue(database.contains("MIGRATION_14_15,"))
        assertTrue(database.contains("MIGRATION_UNSCOPED_OUTBOX_ROW"))
        assertFalse(database.contains("fallbackToDestructiveMigration"))
    }

    @Test
    fun outboxSynchronizer_usesProcessorInsteadOfUnboundedLoop() {
        val manager = source("core/sync/data/OutboxSynchronizer.kt")

        assertTrue(manager.contains("PendingOperationProcessor("))
        assertTrue(manager.contains("PermanentOutboxException"))
        assertFalse(manager.contains("dao.getAll().forEach"))
        assertFalse(manager.contains("dao.incrementRetry"))
    }

    private fun source(relativePath: String): String =
        ProjectLayout.source(relativePath).readText()

    private fun locateProjectRoot(): File {
        var current = File(System.getProperty("user.dir")).absoluteFile
        repeat(8) {
            if (File(current, "settings.gradle.kts").isFile) return current
            current = current.parentFile ?: return@repeat
        }
        error("Project root not found")
    }
}
