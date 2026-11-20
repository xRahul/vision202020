package `in`.rahulja.vision202020.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import `in`.rahulja.vision202020.data.UserPreferences
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.ui.main.MainScreen
import `in`.rahulja.vision202020.ui.theme.VisionTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @Config(qualifiers = "w392dp-h800dp-port")
    fun testMainScreenRendersInPortrait() {
        var startClicked = false

        composeTestRule.setContent {
            VisionTheme {
                MainScreen(
                    status = TimerStatus.Idle,
                    preferences = UserPreferences(),
                    hasOverlayPermission = true,
                    hasNotificationPermission = true,
                    showFullScreenIntentCard = false,
                    onStart = { startClicked = true },
                    onPause = {},
                    onResume = {},
                    onReset = {},
                    onStop = {},
                    onTriggerBreak = {},
                    onOpenSettings = {},
                    onRequestOverlayPermission = {},
                    onRequestNotificationPermission = {},
                    onRequestFullScreenIntent = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Vision 20-20-20").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mobile Screen Guard Active").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Session").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithText("Start Session").performClick()
        assertTrue("onStart should be triggered", startClicked)
    }

    @Test
    @Config(qualifiers = "w800dp-h392dp-land")
    fun testMainScreenRendersInLandscape() {
        var pauseClicked = false

        composeTestRule.setContent {
            VisionTheme {
                MainScreen(
                    status = TimerStatus.Working(
                        remainingSeconds = 1100,
                        elapsedSeconds = 100,
                        totalSeconds = 1200,
                    ),
                    preferences = UserPreferences(),
                    hasOverlayPermission = true,
                    hasNotificationPermission = true,
                    showFullScreenIntentCard = false,
                    onStart = {},
                    onPause = { pauseClicked = true },
                    onResume = {},
                    onReset = {},
                    onStop = {},
                    onTriggerBreak = {},
                    onOpenSettings = {},
                    onRequestOverlayPermission = {},
                    onRequestNotificationPermission = {},
                    onRequestFullScreenIntent = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Vision 20-20-20").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mobile Screen Guard Active").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pause").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rest Now").assertIsDisplayed()

        composeTestRule.onNodeWithText("Pause").performClick()
        assertTrue("onPause should be triggered", pauseClicked)
    }
}
