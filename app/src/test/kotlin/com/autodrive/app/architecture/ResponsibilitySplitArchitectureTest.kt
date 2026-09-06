package com.autodrive.app.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsibilitySplitArchitectureTest {
    private val moduleDir: File = sequenceOf(
        File(System.getProperty("user.dir")),
        File(System.getProperty("user.dir"), "app"),
    ).first { File(it, "src/main").isDirectory }
    private val root = ProjectLayout.mergedAppRoot

    @Test fun `priority entry files remain focused`() {
        val limits = mapOf(
            "feature/chat/presentation/ChatScreen.kt" to 220,
            "feature/home/presentation/HomeScreen.kt" to 220,
            "feature/balance/presentation/BalanceScreen.kt" to 280,
            "navigation/AppNavigation.kt" to 80,
            "feature/chat/data/ChatRepositoryImpl.kt" to 380,
            "core/sync/data/SyncManager.kt" to 280,
        )
        val offenders = limits.filter { (path, limit) ->
            root.resolve(path).readLines().size > limit
        }
        assertTrue("Oversized responsibility entry files: $offenders", offenders.isEmpty())
    }

    @Test fun `chat route does not own recording or gallery persistence`() {
        val source = root.resolve("feature/chat/presentation/ChatScreen.kt").readText()
        assertFalse(source.contains("MediaRecorder"))
        assertFalse(source.contains("MediaStore"))
        assertFalse(source.contains("ContentValues"))
    }

    @Test fun `chat presentation has no detached io scope`() {
        val source = root.resolve("feature/chat/presentation").walkTopDown()
            .filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
        assertFalse(source.contains("CoroutineScope(Dispatchers.IO + SupervisorJob())"))
    }

    @Test fun `chat repository delegates media infrastructure`() {
        val source = root.resolve("feature/chat/data/ChatRepositoryImpl.kt").readText()
        assertTrue(source.contains("ChatMediaManager"))
        assertTrue(source.contains("prepareOutgoing"))
        assertFalse(source.contains("import android.media"))
        assertFalse(source.contains("HttpClient"))
    }

    @Test fun `sync manager delegates secondary responsibilities`() {
        val source = root.resolve("core/sync/data/SyncManager.kt").readText()
        assertTrue(source.contains("PresenceReporter"))
        assertTrue(source.contains("OutboxSynchronizer"))
        assertTrue(source.contains("LocalDataCleaner"))
        assertFalse(source.contains("PendingOperationProcessor"))
        assertFalse(source.contains("RequestWithdrawalParams"))
    }

    @Test fun `app navigation only composes feature graphs`() {
        val source = root.resolve("navigation/AppNavigation.kt").readText()
        assertTrue(source.contains("authAndRegistrationGraph"))
        assertTrue(source.contains("mainGraph"))
        assertTrue(source.contains("infoAndChatGraph"))
        assertFalse(source.contains("composable("))
        assertFalse(source.contains("sealed class Screen"))
    }

    @Test fun `design system uses canonical responsibility packages only`() {
        listOf(
            "core/designsystem/components/SharedComponents.kt",
            "core/designsystem/components/BottomNavigationComponents.kt",
            "core/designsystem/components/SevenSegment.kt",
            "core/designsystem/theme/Typography.kt",
        ).forEach { legacyPath ->
            assertFalse("Legacy design-system file restored: $legacyPath", root.resolve(legacyPath).exists())
        }

        listOf(
            "core/designsystem/components/actions/ActionComponents.kt",
            "core/designsystem/components/inputs/InputComponents.kt",
            "core/designsystem/components/containers/ContainerComponents.kt",
            "core/designsystem/components/navigation/NavigationComponents.kt",
            "core/designsystem/components/feedback/FeedbackComponents.kt",
            "core/designsystem/components/data/DataComponents.kt",
        ).forEach { canonicalPath ->
            assertTrue("Missing canonical design-system file: $canonicalPath", root.resolve(canonicalPath).isFile)
        }

        val theme = root.resolve("core/designsystem/theme/Theme.kt").readText()
        listOf("BgDeep", "TextPrimary", "AccentBlue", "OrangeAccent").forEach { alias ->
            assertFalse("Legacy theme alias restored: $alias", theme.contains("val $alias"))
        }
    }
}
