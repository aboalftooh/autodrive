package com.autodrive.app.architecture

import java.io.File

/** Creates a read-only merged view for architecture tests after Gradle modularization. */
object ProjectLayout {
    val projectRoot: File by lazy {
        var current = File(System.getProperty("user.dir")).absoluteFile
        repeat(8) {
            if (File(current, "settings.gradle.kts").isFile) return@lazy current
            current = current.parentFile ?: return@repeat
        }
        error("Project root not found")
    }

    val appModule: File get() = projectRoot.resolve("app")

    val productionRoots: List<File> by lazy {
        buildList {
            add(projectRoot.resolve("app/src/main/kotlin/com/autodrive/app"))
            listOf("core", "feature").forEach { group ->
                projectRoot.resolve(group).listFiles()
                    .orEmpty()
                    .filter(File::isDirectory)
                    .map { it.resolve("src/main/kotlin/com/autodrive/app") }
                    .filter(File::isDirectory)
                    .forEach(::add)
            }
        }
    }

    val mergedAppRoot: File by lazy {
        val output = projectRoot.resolve("build/architecture-sources/com/autodrive/app")
        output.deleteRecursively()
        output.mkdirs()
        productionRoots.forEach { sourceRoot ->
            sourceRoot.walkTopDown()
                .filter(File::isFile)
                .forEach { source ->
                    val target = output.resolve(source.relativeTo(sourceRoot).path)
                    target.parentFile.mkdirs()
                    source.copyTo(target, overwrite = true)
                }
        }
        output
    }

    fun source(relativePath: String): File = mergedAppRoot.resolve(relativePath)
    fun allProductionFiles(): Sequence<File> = productionRoots.asSequence()
        .flatMap { it.walkTopDown().asSequence() }
        .filter { it.isFile && it.extension == "kt" }
}
