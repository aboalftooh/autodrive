package com.autodrive.app.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionIsolationArchitectureTest {

    private val moduleDir: File = sequenceOf(
        File(System.getProperty("user.dir")),
        File(System.getProperty("user.dir"), "app"),
    ).first { File(it, "src/main").isDirectory }

    private val appRoot = ProjectLayout.mergedAppRoot

    @Test
    fun `domain and presentation do not import PreferencesManager`() {
        val roots = listOf(
            appRoot.resolve("core/common"),
            appRoot.resolve("core/model"),
            appRoot.resolve("core/session/domain"),
            appRoot.resolve("core/sync/domain"),
        ) + appRoot.resolve("feature").listFiles().orEmpty().flatMap {
            listOf(it.resolve("domain"), it.resolve("presentation"))
        }
        val offenders = roots.flatMap { root ->
            if (!root.exists()) emptyList() else root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .filter { it.readText().contains("com.autodrive.app.core.session.data.PreferencesManager") }
                .toList()
        }

        assertFalse(
            "PreferencesManager يجب أن يبقى تفصيل تخزين فقط: ${offenders.joinToString()}",
            offenders.isNotEmpty(),
        )
    }

    @Test
    fun `session contracts remain pure Kotlin`() {
        val sessionFiles = appRoot.resolve("core/session/domain")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        assertTrue(sessionFiles.isNotEmpty())
        val forbidden = listOf("import android.", "import androidx.", "PreferencesManager")
        val offenders = sessionFiles.filter { file ->
            forbidden.any { token -> file.readText().contains(token) }
        }
        assertFalse("عقود الجلسة يجب أن تكون Kotlin خالصة: ${offenders.joinToString()}", offenders.isNotEmpty())
    }

    @Test
    fun `notification DAO scopes every private operation by user id`() {
        val source = appRoot.resolve("core/database/dao/NotificationDao.kt").readText()

        listOf(
            "observeByUserId(userId: String)",
            "observeUnreadCount(userId: String)",
            "markAsRead(id: String, userId: String)",
            "markAllAsRead(userId: String)",
            "confirmReadSynced(id: String, userId: String)",
            "confirmAllReadSynced(userId: String)",
            "getUnsynced(userId: String)",
            "deleteByUserId(userId: String)",
        ).forEach { signature -> assertTrue("مفقود: $signature", source.contains(signature)) }

        assertFalse(source.contains("SELECT * FROM notifications ORDER BY"))
        assertFalse(source.contains("@Query(\"DELETE FROM notifications\")"))
    }

    @Test
    fun `sign out cleanup removes all account scoped caches`() {
        val source = appRoot.resolve("core/sync/data/LocalDataCleaner.kt").readText()

        listOf(
            "paymentDao().deleteByInvoiceIds",
            "notificationDao().deleteByUserId(scope.userId)",
            "chatMessageDao().deleteByConversationIds",
            "conversationDao().deleteByMarketer(scope.userId)",
            "weeklyLeaderboardDao().clear()",
            "pendingOperationDao().deleteForScope",
            "syncCursorDao().deleteForScope",
            "db.withTransaction",
        ).forEach { required -> assertTrue("تنظيف الخروج ناقص: $required", source.contains(required)) }
    }
}
