package com.autodrive.app.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseSafetyArchitectureTest {

    private val moduleDir: File = sequenceOf(
        File(System.getProperty("user.dir")),
        File(System.getProperty("user.dir"), "app"),
    ).first { File(it, "src/main").isDirectory }

    @Test
    fun `Room schema export remains enabled`() {
        val databaseFile = ProjectLayout.source("core/database/AutoDriveDatabase.kt")
        val buildFile = ProjectLayout.projectRoot.resolve("core/database/build.gradle.kts")

        assertTrue(databaseFile.readText().contains("exportSchema = true"))
        assertTrue(buildFile.readText().contains("room.schemaLocation"))
    }

    @Test
    fun `destructive migration fallback is forbidden`() {
        val productionKotlin = ProjectLayout.allProductionFiles().toList()

        val offenders = productionKotlin.filter {
            it.readText().contains("fallbackToDestructiveMigration")
        }

        assertFalse(
            "يمنع حذف قاعدة المستخدم تلقائياً. الملفات المخالفة: ${offenders.joinToString()}",
            offenders.isNotEmpty(),
        )
    }

    @Test
    fun `all declared migrations are registered through one list`() {
        val databaseFile = ProjectLayout.source("core/database/AutoDriveDatabase.kt").readText()
        val moduleFile = ProjectLayout.source("di/AppModule.kt").readText()

        assertTrue(databaseFile.contains("val ALL_MIGRATIONS: Array<Migration>"))
        assertTrue(moduleFile.contains("addMigrations(*AutoDriveDatabase.ALL_MIGRATIONS)"))
    }
}
