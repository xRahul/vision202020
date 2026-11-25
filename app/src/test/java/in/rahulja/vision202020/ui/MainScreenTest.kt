package `in`.rahulja.vision202020.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import `in`.rahulja.vision202020.data.UserPreferences
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.ui.main.MainScreen
import `in`.rahulja.vision202020.ui.theme.VisionTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
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
    @Config(qualifiers = "w392dp-h800dp-port")
    fun notificationAndBatteryCardButtonsInvokeCallbacks() {
        var notificationClicks = 0
        var batteryClicks = 0

        composeTestRule.setContent {
            VisionTheme {
                MainScreen(
                    status = TimerStatus.Idle,
                    preferences = UserPreferences(),
                    hasOverlayPermission = true,
                    hasNotificationPermission = false,
                    showFullScreenIntentCard = false,
                    showBatteryCard = true,
                    onStart = {},
                    onPause = {},
                    onResume = {},
                    onReset = {},
                    onStop = {},
                    onTriggerBreak = {},
                    onOpenSettings = {},
                    onRequestOverlayPermission = {},
                    onRequestNotificationPermission = { notificationClicks++ },
                    onRequestFullScreenIntent = {},
                    onRequestBatteryExemption = { batteryClicks++ }
                )
            }
        }

        composeTestRule.onNodeWithText("Enable Notifications").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Battery Settings").performScrollTo().performClick()

        assertEquals(1, notificationClicks)
        assertEquals(1, batteryClicks)
    }

    @Test
    @Config(qualifiers = "w392dp-h800dp-port")
    fun blockedBreakChannelCardOpensChannelSettings() {
        var channelClicks = 0

        composeTestRule.setContent {
            VisionTheme {
                MainScreen(
                    status = TimerStatus.Idle,
                    preferences = UserPreferences(),
                    hasOverlayPermission = true,
                    hasNotificationPermission = true,
                    showFullScreenIntentCard = false,
                    showBreakChannelCard = true,
                    onStart = {},
                    onPause = {},
                    onResume = {},
                    onReset = {},
                    onStop = {},
                    onTriggerBreak = {},
                    onOpenSettings = {},
                    onRequestOverlayPermission = {},
                    onRequestNotificationPermission = {},
                    onRequestFullScreenIntent = {},
                    onOpenBreakChannelSettings = { channelClicks++ },
                    onRequestBatteryExemption = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Open Alert Settings").performScrollTo().performClick()

        assertEquals(1, channelClicks)
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
                        totalSeconds = 1200
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

    @Test
    @Config(qualifiers = "w392dp-h800dp-port")
    fun timerHasSpokenDescription() {
        composeTestRule.setContent {
            VisionTheme {
                MainScreen(
                    status = TimerStatus.Working(remainingSeconds = 330, elapsedSeconds = 0, totalSeconds = 1200),
                    preferences = UserPreferences(),
                    hasOverlayPermission = true,
                    hasNotificationPermission = true,
                    showFullScreenIntentCard = false,
                    onStart = {},
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

        composeTestRule.onNodeWithContentDescription("6 minutes remaining").assertExists()
    }

    @Test
    @Config(qualifiers = "w392dp-h800dp-port")
    fun spokenDescriptionRoundsUpToWholeMinutesAboveSixtySeconds() {
        var remaining by mutableIntStateOf(330)
        composeTestRule.setContent {
            VisionTheme {
                MainScreen(
                    status = TimerStatus.Working(remainingSeconds = remaining, elapsedSeconds = 0, totalSeconds = 1200),
                    preferences = UserPreferences(),
                    hasOverlayPermission = true,
                    hasNotificationPermission = true,
                    showFullScreenIntentCard = false,
                    onStart = {},
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

        composeTestRule.onNodeWithContentDescription("6 minutes remaining").assertExists()
        remaining = 301
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("6 minutes remaining").assertExists()
        remaining = 300
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("5 minutes remaining").assertExists()
        remaining = 299
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("5 minutes remaining").assertExists()
        remaining = 119
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("2 minutes remaining").assertExists()
        remaining = 61
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("2 minutes remaining").assertExists()
        remaining = 59
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("59 seconds remaining").assertExists()
    }
}
