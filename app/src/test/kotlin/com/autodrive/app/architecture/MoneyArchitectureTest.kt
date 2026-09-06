package com.autodrive.app.architecture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MoneyArchitectureTest {
    private val moduleDir: File = sequenceOf(
        File(System.getProperty("user.dir")),
        File(System.getProperty("user.dir"), "app"),
    ).first { File(it, "src/main").isDirectory }
    private val root = ProjectLayout.mergedAppRoot

    @Test
    fun financialDomainModels_doNotUseDouble() {
        val files = listOf(
            "core/model/account/AccountModels.kt",
            "feature/commission/domain/model/CommissionModels.kt",
            "feature/commission/domain/model/Invoice.kt",
            "feature/commission/domain/model/Payment.kt",
            "feature/commission/domain/model/CommissionPayment.kt",
            "feature/competition/domain/model/WeeklyCompetition.kt",
            "feature/balance/domain/model/BalanceModels.kt",
            "feature/balance/domain/repository/BalanceRepository.kt",
        )
        files.forEach { path -> assertFalse(path, File(root, path).readText().contains("Double")) }
    }

    @Test
    fun roomFinancialEntities_useBigDecimal() {
        val entities = File(root, "core/database/entities/Entities.kt").readText()
        assertTrue(entities.contains("val commission: BigDecimal"))
        assertTrue(entities.contains("val amount: BigDecimal"))
        assertTrue(entities.contains("val balance: BigDecimal"))
        assertFalse(entities.contains("val amount: Double"))
    }

    @Test
    fun supabaseFinancialDtos_useBigDecimalSerializer() {
        val dtoText = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.invariantSeparatorsPath.contains("/core/network/dto/") }
            .joinToString("\n") { it.readText() }
        assertFalse(dtoText.contains("val amount: Double"))
        assertFalse(dtoText.contains("val totalAmount: Double"))
        assertTrue(dtoText.contains("BigDecimalSerializer"))
    }

    @Test
    fun currentDatabase_retainsExactMoneyMigration() {
        val db = File(root, "core/database/AutoDriveDatabase.kt").readText()
        assertTrue(db.contains("AUTODRIVE_DATABASE_VERSION = 17"))
        assertTrue(db.contains("MIGRATION_11_12"))
        assertTrue(db.contains("CAST(amount AS TEXT)"))
        assertTrue(db.contains("CAST(balance AS TEXT)"))
        assertTrue(db.contains("MIGRATION_11_12,"))
    }

    @Test
    fun withdrawalContract_acceptsMoney() {
        val contract = File(root, "feature/balance/domain/repository/BalanceRepository.kt").readText()
        assertTrue(contract.contains("requestWithdrawal(amount: Money"))
        assertFalse(contract.contains("requestWithdrawal(amount: Double"))
    }

    @Test
    fun doubleConversion_isRestrictedToPresentationOrLegacyCompatibility() {
        val offenders = root.walkTopDown()
            .filter { it.extension == "kt" }
            .filterNot {
                val path = it.invariantSeparatorsPath
                path.contains("core/designsystem/components/DonutChart.kt") ||
                    path.contains("feature/home/presentation/HomeHeroComponents.kt") ||
                    path.contains("core/model/money/Money.kt") ||
                    path.contains("core/session/data/PreferencesManager.kt") ||
                    path.contains("core/sync/outbox/OutboxRetryPolicy.kt") ||
                    path.contains("feature/home/presentation/audio/BenzineSound.kt")
            }
            .filter { it.readText().contains("toDouble()") }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .toList()
        assertTrue("Unexpected toDouble(): $offenders", offenders.isEmpty())
    }
}
