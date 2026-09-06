package com.autodrive.app.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncBoundaryArchitectureTest {

    private val moduleDir: File = sequenceOf(
        File(System.getProperty("user.dir")),
        File(System.getProperty("user.dir"), "app")
    ).first { File(it, "src/main").isDirectory }

    private val appRoot = ProjectLayout.mergedAppRoot

    @Test
    fun `view models do not import SyncManager`() {
        val offenders = appRoot.resolve("feature").walkTopDown()
            .filter { it.isFile && it.name.endsWith("ViewModel.kt") }
            .filter { it.readText().contains("com.autodrive.app.core.sync.data.SyncManager") }
            .toList()

        assertFalse("ViewModel يجب أن يعتمد على SyncCoordinator فقط: $offenders", offenders.isNotEmpty())
    }

    @Test
    fun `all full synchronization requests use reasons`() {
        val sources = appRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        val directFullSync = sources.filter { it.readText().contains(".fullSync()") }

        assertTrue(
            "يجب وجود أسباب المزامنة داخل العقد",
            appRoot.resolve("core/sync/domain/SyncCoordinator.kt")
                .readText().contains("enum class SyncReason")
        )
        assertFalse("استدعاءات fullSync القديمة ما زالت موجودة: $directFullSync", directFullSync.isNotEmpty())
    }

    @Test
    fun `sync contract remains pure Kotlin`() {
        val files = appRoot.resolve("core/sync/domain")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        val offenders = files.filter { file ->
            val text = file.readText()
            text.contains("import android.") ||
                text.contains("import androidx.") ||
                text.contains("com.autodrive.app.core.sync.data")
        }

        assertTrue(files.isNotEmpty())
        assertFalse("عقود المزامنة يجب أن تبقى Kotlin خالصة: $offenders", offenders.isNotEmpty())
    }
}
