package com.autodrive.app.designsystem.verification

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.home.presentation.AiInsightCard
import com.autodrive.app.feature.home.presentation.HomeUiState
import com.autodrive.app.feature.home.presentation.NotificationBell
import com.autodrive.app.feature.home.presentation.PumpHeroCard
import com.autodrive.app.feature.home.presentation.WeeklyCompetitionTeaser
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeV61ContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun notificationAndCompetitionKeepInteractionContracts() {
        var notificationClicks = 0
        var competitionClicks = 0
        composeRule.setContent {
            AutoDriveTheme {
                Column {
                    NotificationBell(unreadCount = 0, onClick = { notificationClicks += 1 })
                    WeeklyCompetitionTeaser(
                        description = "قريباً",
                        onClick = { competitionClicks += 1 },
                        modifier = Modifier.testTag("competition"),
                    )
                    AiInsightCard(
                        dynamoMessage = "رسالة ثابتة",
                        modifier = Modifier.testTag("insight"),
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("الإشعارات").assertHasClickAction().performClick()
        composeRule.onNodeWithTag("competition").assertHasClickAction().performClick()
        composeRule.onNodeWithText("قريباً")
        composeRule.onNodeWithText("نافذة بنزين").assertExists()
        composeRule.runOnIdle {
            assertEquals(1, notificationClicks)
            assertEquals(1, competitionClicks)
        }
    }

    @Test
    fun pumpIsDisabledWhilePumping() {
        var pumpClicks = 0
        composeRule.setContent {
            AutoDriveTheme {
                PumpHeroCard(
                    state = HomeUiState(
                        isLoading = false,
                        isPumping = true,
                        displayedTotal = Money.of(100_000L),
                        syncedTotal = Money.of(100_000L),
                        weeklyTarget = Money.of(500_000L),
                        nextFriday9AmMs = System.currentTimeMillis() + 60_000L,
                    ),
                    onPump = { pumpClicks += 1 },
                    onPumpAnimationComplete = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("ضخ البنزين").assertIsNotEnabled()
        composeRule.runOnIdle { assertEquals(0, pumpClicks) }
    }

    @Test
    fun aiInsightRemainsPassive() {
        composeRule.setContent {
            AutoDriveTheme {
                AiInsightCard("رسالة ثابتة", Modifier.testTag("insight"))
            }
        }
        composeRule.onNodeWithTag("insight").assertExists()
        composeRule.onNodeWithContentDescription("ضخ البنزين").assertDoesNotExist()
    }
}
