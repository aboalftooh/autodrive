package com.autodrive.app.designsystem.verification

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsV63ContractTest {
    private fun source(relative: String): String =
        (File(relative).takeIf { it.exists() } ?: File("../$relative")).readText()

    @Test
    fun `destructive settings row forwards governed Error tone`() {
        val settings = source("core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt")
        assertTrue(settings.contains("titleTone = if (variant == SettingsRowVariant.Destructive) AutoDriveStatusTone.Error else null"))
        assertFalse(settings.contains("val titleColor ="))
        assertTrue(settings.contains("Icons.AutoMirrored.Rounded.KeyboardArrowLeft"))
        assertTrue(settings.contains("AutoDriveStatusChip(value, statusTone)"))
    }

    @Test
    fun `list row gives disabled state precedence and blocks clicks`() {
        val data = source("core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt")
        assertTrue(data.contains("titleTone: AutoDriveStatusTone? = null"))
        assertTrue(data.contains("if (!enabled) AutoDriveText.Disabled else titleTone?.color() ?: AutoDriveText.Primary"))
        assertTrue(data.contains("if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier"))
        assertTrue(data.contains("if (!enabled) Modifier.semantics { disabled() } else Modifier"))
        assertFalse(data.contains("titleColor: Color"))
    }

    @Test
    fun `settings group keeps divider status and callback ownership`() {
        val settings = source("core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt")
        assertTrue(settings.contains("if (index != items.lastIndex) AutoDriveDivider()"))
        assertTrue(settings.contains("onItemClick: (String) -> Unit"))
        assertTrue(settings.contains("onClick = { onItemClick(item.id) }"))
        assertFalse(settings.contains("SettingsRowVariant.Toggle"))
    }
}
