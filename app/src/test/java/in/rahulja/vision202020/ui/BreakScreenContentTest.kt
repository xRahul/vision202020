package `in`.rahulja.vision202020.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import `in`.rahulja.vision202020.ui.breakui.BreakScreenContent
import `in`.rahulja.vision202020.ui.theme.VisionTheme
import org.junit.Assert.assertFalse
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

    @Test
    @Config(qualifiers = "w392dp-h800dp-port")
    fun strictSkipHasAccessibilityAction() {
        var skipped = false
        composeTestRule.setContent {
            VisionTheme {
                BreakScreenContent(
                    remainingSeconds = 18,
                    totalSeconds = 20,
                    strictMode = true,
                    onSkip = { skipped = true },
                    onSnooze = {}
                )
            }
        }

        val node = composeTestRule.onNodeWithContentDescription("Hold 2s to Skip")
        val action = node.fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .single { it.label == "Skip break…" }

        composeTestRule.runOnUiThread { action.action() }
        composeTestRule.waitForIdle()
        assertFalse("opening the confirm must not skip", skipped)
        composeTestRule.onNodeWithText("Skip this break?").assertIsDisplayed()
        assertTrue(
            composeTestRule.onAllNodes(androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsProperties.PaneTitle))
                .fetchSemanticsNodes().any { it.config[SemanticsProperties.PaneTitle] == "Skip this break?" }
        )

        composeTestRule.onNodeWithText("Keep resting").performClick()
        assertFalse("cancel must not skip", skipped)
        composeTestRule.onNodeWithText("Skip this break?").assertDoesNotExist()

        composeTestRule.runOnUiThread { action.action() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Skip").performClick()
        assertTrue("confirming must call onSkip", skipped)
    }

    @Test
    @Config(qualifiers = "w392dp-h800dp-port")
    fun countdownIsPoliteLiveRegion() {
        var remaining by mutableIntStateOf(15)
        composeTestRule.setContent {
            VisionTheme {
                BreakScreenContent(
                    remainingSeconds = remaining,
                    totalSeconds = 20,
                    strictMode = false,
                    onSkip = {},
                    onSnooze = {}
                )
            }
        }

        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        composeTestRule.onNode(polite).assertExists()
        composeTestRule.onNodeWithContentDescription("15 seconds remaining").assertExists()
        // Same 5-second bucket: no new announcement text.
        remaining = 14
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("15 seconds remaining").assertExists()
        remaining = 10
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("10 seconds remaining").assertExists()
    }
}
