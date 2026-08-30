package com.autodrive.app.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainPresentationBoundaryArchitectureTest {

    private val moduleDir: File = sequenceOf(
        File(System.getProperty("user.dir")),
        File(System.getProperty("user.dir"), "app"),
    ).first { File(it, "src/main").isDirectory }

    private val appRoot = ProjectLayout.mergedAppRoot

    @Test
    fun `domain remains pure Kotlin`() {
        val forbidden = listOf(
            "import android.",
            "import androidx.",
            "import com.autodrive.app.core.designsystem.R",
            "import com.autodrive.app.core.database.",
            "import com.autodrive.app.core.network.",
            "import com.autodrive.app.core.session.data.",
            "import com.autodrive.app.core.sync.data.",
            "import com.autodrive.app.core.sync.outbox.",
            "import com.autodrive.app.core.sync.realtime.",
            "import com.autodrive.app.core.sync.worker.",
        )
        val coreDomainRoots = listOf(
            appRoot.resolve("core/common"),
            appRoot.resolve("core/model"),
            appRoot.resolve("core/session/domain"),
            appRoot.resolve("core/sync/domain"),
        )
        val featureDomainRoots = appRoot.resolve("feature").listFiles().orEmpty()
            .map { it.resolve("domain") }
        val offenders = (coreDomainRoots + featureDomainRoots)
            .flatMap { it.kotlinFiles() }
            .filter { file -> forbidden.any(file.readText()::contains) }

        assertFalse("Domain violations: $offenders", offenders.isNotEmpty())
    }

    @Test
    fun `view models do not import infrastructure`() {
        val forbidden = listOf(
            "import com.autodrive.app.core.database.",
            "import com.autodrive.app.core.network.",
            "import com.autodrive.app.core.session.data.",
            "import com.autodrive.app.core.sync.data.",
            "import com.autodrive.app.core.sync.outbox.",
            "import com.autodrive.app.core.sync.realtime.",
            "import com.autodrive.app.core.sync.worker.",
            "import androidx.work.",
            "import com.google.firebase.",
            "import io.github.jan.supabase.",
            "AutoDriveDatabase",
            "AppDatabase",
        )
        val presentationRoots = appRoot.resolve("feature").listFiles().orEmpty()
            .map { it.resolve("presentation") }
        val offenders = presentationRoots.flatMap { it.kotlinFiles() }
            .filter { it.name.endsWith("ViewModel.kt") }
            .filter { file -> forbidden.any(file.readText()::contains) }

        assertFalse("Presentation infrastructure violations: $offenders", offenders.isNotEmpty())
    }

    @Test
    fun `dynamo display mapping belongs to presentation`() {
        val domain = appRoot.resolve("feature/home/domain/model/DynamoState.kt").readText()
        val mapper = appRoot.resolve("feature/home/presentation/DynamoStateUiMapper.kt").readText()

        assertFalse(domain.contains("R.drawable"))
        assertFalse(domain.contains("arabicLabel"))
        assertFalse(domain.contains("imageRes"))
        assertTrue(mapper.contains("fun DynamoState.arabicLabel()"))
        assertTrue(mapper.contains("fun DynamoState.imageRes()"))
    }

    @Test
    fun `invoice detail view model depends on domain use case only`() {
        val viewModel = appRoot.resolve("feature/reports/presentation/log/InvoiceDetailViewModel.kt").readText()

        assertTrue(viewModel.contains("GetInvoiceDetailsUseCase"))
        assertFalse(viewModel.contains("AutoDriveDatabase"))
        assertFalse(viewModel.contains("invoiceDao()"))
        assertFalse(viewModel.contains("Dispatchers.IO"))
    }

    @Test
    fun `invoice detail database access is hidden behind repository contract`() {
        val contract = appRoot.resolve("feature/reports/domain/repository/InvoiceDetailRepository.kt").readText()
        val adapter = appRoot.resolve("feature/reports/data/InvoiceDetailRepositoryImpl.kt").readText()
        val module = appRoot.resolve("feature/reports/di/ReportsFeatureModule.kt").readText()

        assertTrue(contract.contains("interface InvoiceDetailRepository"))
        assertFalse(contract.contains("AutoDriveDatabase"))
        assertTrue(adapter.contains("AutoDriveDatabase"))
        assertTrue(module.contains("bindInvoiceDetailRepository"))
    }

    private fun File.kotlinFiles(): List<File> = if (!exists()) emptyList() else walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .toList()
}
