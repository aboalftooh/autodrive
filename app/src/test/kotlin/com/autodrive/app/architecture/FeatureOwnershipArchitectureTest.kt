package com.autodrive.app.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureOwnershipArchitectureTest {

    private val moduleDir: File = sequenceOf(
        File(System.getProperty("user.dir")),
        File(System.getProperty("user.dir"), "app"),
    ).first { File(it, "src/main").isDirectory }

    private val appRoot = ProjectLayout.mergedAppRoot
    private val featureRoot = appRoot.resolve("feature")
    private val features = setOf(
        "auth",
        "profile",
        "home",
        "commission",
        "balance",
        "notifications",
        "chat",
        "competition",
        "reports",
    )

    @Test
    fun `all owned features expose presentation domain data and di layers`() {
        val missing = features.flatMap { feature ->
            listOf("presentation", "domain", "data", "di")
                .filterNot { featureRoot.resolve("$feature/$it").isDirectory }
                .map { "$feature/$it" }
        }
        assertTrue("Missing feature layers: $missing", missing.isEmpty())
    }

    @Test
    fun `feature package declarations match their physical paths`() {
        val offenders = featureRoot.kotlinFiles().mapNotNull { file ->
            val declared = file.readLines()
                .firstOrNull { it.startsWith("package ") }
                ?.removePrefix("package ")
                ?: return@mapNotNull file.relativeTo(appRoot).path
            val expected = "com.autodrive.app." + file.parentFile.relativeTo(appRoot)
                .invariantSeparatorsPath.replace('/', '.')
            if (declared == expected) null else "${file.name}: $declared != $expected"
        }
        assertTrue("Package/path mismatches: $offenders", offenders.isEmpty())
    }

    @Test
    fun `features never import another feature data or di implementation`() {
        val offenders = featureRoot.kotlinFiles().flatMap { file ->
            val owner = file.relativeTo(featureRoot).invariantSeparatorsPath.substringBefore('/')
            file.readLines().filter { line ->
                line.startsWith("import com.autodrive.app.feature.") &&
                    (line.contains(".data.") || line.contains(".di.")) &&
                    !line.startsWith("import com.autodrive.app.feature.$owner.")
            }.map { "${file.relativeTo(featureRoot).path}: $it" }
        }
        assertTrue("Cross-feature concrete dependencies: $offenders", offenders.isEmpty())
    }

    @Test
    fun `feature domain and presentation do not import infrastructure`() {
        val forbidden = listOf(
            "com.autodrive.app.core.database",
            "AutoDriveDatabase",
            "io.github.jan.supabase",
            "androidx.work",
            "com.google.firebase",
        )
        val roots = features.flatMap { feature ->
            listOf(
                featureRoot.resolve("$feature/domain"),
                featureRoot.resolve("$feature/presentation"),
            )
        }
        val offenders = roots.flatMap { it.kotlinFiles() }.filter { file ->
            forbidden.any(file.readText()::contains)
        }
        assertTrue("Infrastructure leaked into domain/presentation: $offenders", offenders.isEmpty())
    }

    @Test
    fun `feature di modules bind only their own repository implementations`() {
        val offenders = features.flatMap { feature ->
            featureRoot.resolve("$feature/di").kotlinFiles().filter { file ->
                val text = file.readText()
                text.lineSequence().filter { it.startsWith("import com.autodrive.app.feature.") }
                    .any { !it.startsWith("import com.autodrive.app.feature.$feature.") }
            }
        }
        assertTrue("DI ownership violations: $offenders", offenders.isEmpty())
    }

    @Test
    fun `legacy feature ownership locations are empty`() {
        val forbidden = listOf(
            "ui/screens/login",
            "ui/screens/join",
            "ui/screens/register",
            "ui/screens/splash",
            "ui/screens/profile",
            "ui/screens/home",
            "ui/screens/balance",
            "ui/screens/notifications",
            "ui/screens/chat",
            "ui/screens/competition",
            "ui/screens/log",
            "ui/screens/recent",
        )
        val existing = forbidden.filter { appRoot.resolve(it).exists() }
        assertTrue("Legacy feature paths still exist: $existing", existing.isEmpty())
    }

    @Test
    fun `home consumes other features through domain contracts only`() {
        val offenders = featureRoot.resolve("home").kotlinFiles().flatMap { file ->
            file.readLines().filter { line ->
                line.startsWith("import com.autodrive.app.feature.") &&
                    !line.startsWith("import com.autodrive.app.feature.home.") &&
                    !line.contains(".domain.")
            }.map { "${file.name}: $it" }
        }
        assertTrue("Home non-domain feature dependencies: $offenders", offenders.isEmpty())
    }

    @Test
    fun `chat owns conversation and message models`() {
        val model = featureRoot.resolve("chat/domain/model/ActivityMessage.kt")
        val text = model.readText()
        assertTrue(text.contains("data class Conversation"))
        assertTrue(text.contains("data class ChatMessage"))
        assertTrue(text.contains("enum class MessageType"))
        assertFalse(featureRoot.resolve("reports/domain/model/ActivityMessage.kt").exists())
    }

    @Test
    fun `central repository module is removed`() {
        assertFalse(appRoot.resolve("di/RepositoryModule.kt").exists())
        features.forEach { feature ->
            assertTrue(
                "Missing DI module for $feature",
                featureRoot.resolve("$feature/di").kotlinFiles().isNotEmpty(),
            )
        }
    }

    private fun File.kotlinFiles(): List<File> = if (!exists()) emptyList() else walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .toList()
}
