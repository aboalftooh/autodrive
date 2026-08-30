package com.autodrive.app.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageByFeatureArchitectureTest {

    private val moduleDir: File = sequenceOf(
        File(System.getProperty("user.dir")),
        File(System.getProperty("user.dir"), "app"),
    ).first { File(it, "src/main").isDirectory }
    private val kotlinRoot = moduleDir.resolve("src/main/kotlin")
    private val appRoot = kotlinRoot.resolve("com/autodrive/app")
    private val featureRoot = appRoot.resolve("feature")

    @Test
    fun `legacy layer roots are removed`() {
        val forbidden = listOf("data", "domain", "ui", "utils", "notifications", "observability")
        val existing = forbidden.filter { appRoot.resolve(it).exists() }
        assertTrue("Legacy package roots still exist: $existing", existing.isEmpty())
    }

    @Test
    fun `application root contains composition packages only`() {
        val allowedDirectories = setOf("core", "feature", "coordinator", "navigation", "di")
        val actualDirectories = appRoot.listFiles().orEmpty()
            .filter { it.isDirectory }
            .map { it.name }
            .toSet()
        val unexpected = actualDirectories - allowedDirectories
        assertTrue("Unexpected application roots: $unexpected", unexpected.isEmpty())
    }

    @Test
    fun `package declarations match physical paths`() {
        val offenders = listOf(
            moduleDir.resolve("src/main/kotlin"),
            moduleDir.resolve("src/test/kotlin"),
            moduleDir.resolve("src/androidTest/kotlin"),
        ).flatMap { root ->
            if (!root.exists()) emptyList() else root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .mapNotNull { file ->
                    val declared = file.readLines()
                        .firstOrNull { it.startsWith("package ") }
                        ?.removePrefix("package ")
                        ?: return@mapNotNull file.relativeTo(root).path
                    val expected = file.parentFile.relativeTo(root)
                        .invariantSeparatorsPath.replace('/', '.')
                    if (declared == expected) null else "${file.relativeTo(root).path}: $declared != $expected"
                }
                .toList()
        }
        assertTrue("Package/path mismatches: $offenders", offenders.isEmpty())
    }

    @Test
    fun `core never depends on feature or composition packages`() {
        val forbidden = listOf(
            "import com.autodrive.app.feature.",
            "import com.autodrive.app.coordinator.",
            "import com.autodrive.app.navigation.",
            "import com.autodrive.app.di.",
        )
        val offenders = appRoot.resolve("core").kotlinFiles().filter { file ->
            forbidden.any(file.readText()::contains)
        }
        assertTrue("Core outward dependencies: $offenders", offenders.isEmpty())
    }

    @Test
    fun `features use only domain contracts from other features`() {
        val offenders = featureRoot.kotlinFiles().flatMap { file ->
            val owner = file.relativeTo(featureRoot).invariantSeparatorsPath.substringBefore('/')
            file.readLines().filter { line ->
                if (!line.startsWith("import com.autodrive.app.feature.")) return@filter false
                val dependency = line.removePrefix("import com.autodrive.app.feature.")
                val dependencyOwner = dependency.substringBefore('.')
                dependencyOwner != owner && !dependency.substringAfter('.', "").startsWith("domain.")
            }.map { "${file.relativeTo(featureRoot).path}: $it" }
        }
        assertTrue("Cross-feature concrete dependencies: $offenders", offenders.isEmpty())
    }

    @Test
    fun `feature dependency graph has no cycles`() {
        val graph = mutableMapOf<String, MutableSet<String>>()
        featureRoot.kotlinFiles().forEach { file ->
            val owner = file.relativeTo(featureRoot).invariantSeparatorsPath.substringBefore('/')
            file.readLines().filter { it.startsWith("import com.autodrive.app.feature.") }
                .forEach { line ->
                    val dependency = line.removePrefix("import com.autodrive.app.feature.")
                        .substringBefore('.')
                    if (dependency != owner) graph.getOrPut(owner) { linkedSetOf() }.add(dependency)
                }
        }

        val visiting = linkedSetOf<String>()
        val visited = mutableSetOf<String>()
        val cycles = mutableListOf<String>()

        fun visit(node: String, path: List<String>) {
            if (node in visiting) {
                val start = path.indexOf(node).coerceAtLeast(0)
                cycles += (path.drop(start) + node).joinToString(" -> ")
                return
            }
            if (!visited.add(node)) return
            visiting += node
            graph[node].orEmpty().forEach { visit(it, path + node) }
            visiting -= node
        }

        (graph.keys + graph.values.flatten()).forEach { visit(it, emptyList()) }
        assertTrue("Feature dependency cycles: $cycles", cycles.isEmpty())
    }

    @Test
    fun `coordinators do not depend on presentation or feature di`() {
        val coordinatorRoot = appRoot.resolve("coordinator")
        val offenders = coordinatorRoot.kotlinFiles().flatMap { file ->
            file.readLines().filter { line ->
                line.startsWith("import com.autodrive.app.feature.") &&
                    (line.contains(".presentation.") || line.contains(".di."))
            }.map { "${file.relativeTo(coordinatorRoot).path}: $it" }
        }
        assertTrue("Coordinator dependency violations: $offenders", offenders.isEmpty())
    }

    @Test
    fun `informational feature is presentation only`() {
        val infoRoot = featureRoot.resolve("info")
        assertTrue(infoRoot.resolve("presentation").isDirectory)
        assertFalse(infoRoot.resolve("data").exists())
        assertFalse(infoRoot.resolve("domain").exists())
        assertFalse(infoRoot.resolve("di").exists())
    }

    private fun File.kotlinFiles(): List<File> = if (!exists()) emptyList() else walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .toList()
}
