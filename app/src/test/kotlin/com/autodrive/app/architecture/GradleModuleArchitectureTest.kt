package com.autodrive.app.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GradleModuleArchitectureTest {
    private val root = ProjectLayout.projectRoot
    private val modules = listOf(
        "core:model", "core:common", "core:database", "core:network",
        "core:observability", "core:session", "core:sync", "core:designsystem", "core:platform",
        "feature:auth", "feature:chat", "feature:notifications",
        "feature:commission", "feature:balance", "feature:profile",
    )

    @Test
    fun `settings declares every extracted module`() {
        val settings = root.resolve("settings.gradle.kts").readText()
        modules.forEach { assertTrue("Missing :$it", settings.contains("\":$it\"")) }
    }

    @Test
    fun `every module has an isolated build script and source root`() {
        modules.forEach { module ->
            val directory = root.resolve(module.replace(':', File.separatorChar))
            assertTrue("Missing build script for $module", directory.resolve("build.gradle.kts").isFile)
            assertTrue("Missing source root for $module", directory.resolve("src/main/kotlin").isDirectory)
        }
    }

    @Test
    fun `app composes all extracted modules`() {
        val build = root.resolve("app/build.gradle.kts").readText()
        modules.forEach { assertTrue("App missing $it", build.contains("project(\":$it\")")) }
    }

    @Test
    fun `module dependency graph has no cycles`() {
        val graph = (modules + "app").associateWith { module ->
            val dir = if (module == "app") root.resolve("app") else root.resolve(module.replace(':', File.separatorChar))
            Regex("project\\(\\\":([^\\\"]+)\\\"\\)")
                .findAll(dir.resolve("build.gradle.kts").readText())
                .map { it.groupValues[1] }
                .toSet()
        }
        val cycles = mutableListOf<List<String>>()
        fun visit(node: String, path: List<String>) {
            if (node in path) {
                cycles += path.dropWhile { it != node } + node
                return
            }
            graph[node].orEmpty().forEach { visit(it, path + node) }
        }
        graph.keys.forEach { visit(it, emptyList()) }
        assertTrue("Module cycles: $cycles", cycles.isEmpty())
    }

    @Test
    fun `core build scripts never depend on feature modules`() {
        val offenders = modules.filter { it.startsWith("core:") }.filter { module ->
            root.resolve(module.replace(':', File.separatorChar))
                .resolve("build.gradle.kts")
                .readText()
                .contains("project(\":feature:")
        }
        assertTrue("Core to feature dependencies: $offenders", offenders.isEmpty())
    }

    @Test
    fun `extracted source roots no longer exist under app`() {
        val appPackage = root.resolve("app/src/main/kotlin/com/autodrive/app")
        val duplicates = modules.map { appPackage.resolve(it.replace(':', File.separatorChar)) }
            .filter { directory -> directory.exists() && directory.walkTopDown().any { it.extension == "kt" } }
        assertTrue("Duplicate sources under app: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `Android resources follow Session 06 ownership boundaries`() {
        val designSystemRes = root.resolve("core/designsystem/src/main/res")
        val appRes = root.resolve("app/src/main/res")
        val authRes = root.resolve("feature/auth/src/main/res")

        assertTrue(designSystemRes.resolve("font/tajawal_regular.ttf").isFile)
        assertTrue(appRes.resolve("mipmap-anydpi-v26/ic_launcher.xml").isFile)
        assertTrue(appRes.resolve("xml/file_paths.xml").isFile)
        assertTrue(authRes.resolve("drawable/login_hero.png").isFile)
        assertFalse(designSystemRes.resolve("drawable/login_hero.png").exists())
        assertFalse(designSystemRes.walkTopDown().any { it.name.startsWith("am_dynamo_") })
    }
}
