package com.autodrive.app.designsystem.verification

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.autodrive.app.core.designsystem.components.AutoDriveStatusTone
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButton
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.data.AutoDriveListRow
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveBadge
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveStatusChip
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DesignSystemV60HarnessTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun officialComponentsExposeSemanticsAndInteractionContracts() {
        var primaryClicks = 0
        var rowClicks = 0
        composeRule.setContent {
            AutoDriveTheme {
                Column {
                    AutoDrivePrimaryButton(
                        text = "Enabled",
                        onClick = { primaryClicks += 1 },
                        modifier = Modifier.testTag("primary_enabled"),
                    )
                    AutoDrivePrimaryButton(
                        text = "Disabled",
                        onClick = {},
                        modifier = Modifier.testTag("primary_disabled"),
                        enabled = false,
                    )
                    AutoDriveIconButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = "Refresh fixture",
                        onClick = {},
                        modifier = Modifier.testTag("icon_button"),
                    )
                    AutoDriveStatusChip(
                        text = "Ready",
                        tone = AutoDriveStatusTone.Success,
                        modifier = Modifier.testTag("status_chip"),
                    )
                    AutoDriveBadge(modifier = Modifier.testTag("badge"), count = 3)
                    AutoDriveListRow(
                        title = "Row",
                        modifier = Modifier.testTag("list_row"),
                        selected = true,
                        onClick = { rowClicks += 1 },
                    )
                    Box(
                        Modifier
                            .testTag("selected_semantics_fixture")
                            .size(48.dp)
                            .selectable(selected = true, onClick = {}),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("primary_enabled").assertIsEnabled().assertHasClickAction().performClick()
        composeRule.runOnIdle { assertEquals(1, primaryClicks) }
        composeRule.onNodeWithTag("primary_disabled").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Refresh fixture").assertHasClickAction()
        composeRule.onNodeWithTag("list_row").assertHasClickAction().performClick()
        composeRule.runOnIdle { assertEquals(1, rowClicks) }
        composeRule.onNodeWithTag("selected_semantics_fixture").assertIsSelected().assertHasClickAction()
    }

    @Test
    fun screenshotCaptureWritesImageAndDeterministicMetadata() {
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AutoDriveTheme {
                    Box(
                        modifier = Modifier
                            .testTag("capture_root")
                            .width(360.dp)
                            .height(640.dp),
                    ) {
                        AutoDrivePrimaryButton(text = "Capture", onClick = {})
                    }
                }
            }
        }

        val bitmap = composeRule.onNodeWithTag("capture_root").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputDir = File(context.filesDir, "designsystem-v60-captures").apply { mkdirs() }
        val imageFile = File(outputDir, "v60_primary_button_rtl.png")
        FileOutputStream(imageFile).use { stream ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }

        val config = context.resources.configuration
        val metadataFile = File(outputDir, "v60_primary_button_rtl.json")
        metadataFile.writeText(
            """{
              "fixtureId": "v60_primary_button_rtl",
              "viewportDp": "360x640",
              "deviceScreenDp": "${config.screenWidthDp}x${config.screenHeightDp}",
              "fontScale": ${config.fontScale},
              "layoutDirection": "RTL",
              "sourceProductionManifestSha256": "3b684bd91c390eeade95b5594d8924156465c88be2510caa843540c37c877751",
              "imagePath": "${imageFile.absolutePath.replace("\\", "\\\\")}" 
            }""".trimIndent(),
        )
        assertTrue(imageFile.isFile && imageFile.length() > 0)
        assertTrue(metadataFile.isFile && metadataFile.length() > 0)
    }
}
