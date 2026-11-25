package `in`.rahulja.vision202020.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import `in`.rahulja.vision202020.ui.breakui.BreakScreenContent
import `in`.rahulja.vision202020.ui.theme.VisionTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BreakScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @Config(qualifiers = "w392dp-h800dp-port")
    fun testBreakScreenContentRendersInPortrait() {
        var skipped = false
        var snoozed = false

        composeTestRule.setContent {
            VisionTheme {
                BreakScreenContent(
                    remainingSeconds = 15,
                    totalSeconds = 20,
                    strictMode = false,
                    onSkip = { skipped = true },
                    onSnooze = { snoozed = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Rest Your Eyes").assertIsDisplayed()
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
        composeTestRule.onNodeWithText("Snooze 5m").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip Break").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithText("Skip Break").performClick()
        assertTrue("onSkip should be triggered", skipped)
    }

    @Test
    @Config(qualifiers = "w800dp-h392dp-land")
    fun testBreakScreenContentRendersInLandscape() {
        var snoozed = false

        composeTestRule.setContent {
            VisionTheme {
                BreakScreenContent(
                    remainingSeconds = 12,
                    totalSeconds = 20,
                    strictMode = false,
                    onSkip = {},
                    onSnooze = { snoozed = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Rest Your Eyes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Snooze 5m").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip Break").assertIsDisplayed()
        composeTestRule.onNodeWithText("12").assertIsDisplayed()

        composeTestRule.onNodeWithText("Snooze 5m").performClick()
        assertTrue("onSnooze should be triggered", snoozed)
    }

    @Test
    @Config(qualifiers = "w800dp-h392dp-land")
    fun testBreakScreenContentStrictModeInLandscape() {
        composeTestRule.setContent {
            VisionTheme {
                BreakScreenContent(
                    remainingSeconds = 18,
                    totalSeconds = 20,
                    strictMode = true,
                    onSkip = {},
                    onSnooze = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Rest Your Eyes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Snooze 5m").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hold 2s to Skip").assertIsDisplayed()
    }
}
