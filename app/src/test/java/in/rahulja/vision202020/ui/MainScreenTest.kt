package `in`.rahulja.vision202020.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.data.UserPreferences
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.ui.main.MainScreen
import `in`.rahulja.vision202020.ui.theme.VisionTheme
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainScreenTest {

    @Test
    fun testMainScreenRendersInPortrait() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val portraitConfig = Configuration(context.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
            screenWidthDp = 392
            screenHeightDp = 800
        }
        val portraitContext = context.createConfigurationContext(portraitConfig)

        val composeView = ComposeView(portraitContext).apply {
            setContent {
                VisionTheme {
                    MainScreen(
                        status = TimerStatus.Working(remainingSeconds = 1100, elapsedSeconds = 100, totalSeconds = 1200, isScreenPaused = false),
                        preferences = UserPreferences(),
                        hasOverlayPermission = true,
                        hasNotificationPermission = true,
                        onStart = {},
                        onPause = {},
                        onResume = {},
                        onStop = {},
                        onTriggerBreak = {},
                        onOpenSettings = {},
                        onRequestOverlayPermission = {},
                        onRequestNotificationPermission = {}
                    )
                }
            }
        }
        assertNotNull("MainScreen in portrait should not be null", composeView)
    }

    @Test
    fun testMainScreenRendersInLandscape() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val landscapeConfig = Configuration(context.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
            screenWidthDp = 800
            screenHeightDp = 360
        }
        val landscapeContext = context.createConfigurationContext(landscapeConfig)

        val composeView = ComposeView(landscapeContext).apply {
            setContent {
                VisionTheme {
                    MainScreen(
                        status = TimerStatus.Working(remainingSeconds = 1100, elapsedSeconds = 100, totalSeconds = 1200, isScreenPaused = false),
                        preferences = UserPreferences(),
                        hasOverlayPermission = false,
                        hasNotificationPermission = true,
                        onStart = {},
                        onPause = {},
                        onResume = {},
                        onStop = {},
                        onTriggerBreak = {},
                        onOpenSettings = {},
                        onRequestOverlayPermission = {},
                        onRequestNotificationPermission = {}
                    )
                }
            }
        }
        assertNotNull("MainScreen in landscape should not be null", composeView)
    }
}
