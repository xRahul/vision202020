package `in`.rahulja.vision202020.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.ui.breakui.BreakScreenContent
import `in`.rahulja.vision202020.ui.theme.VisionTheme
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BreakScreenContentTest {

    @Test
    fun testBreakScreenContentRendersInPortrait() {
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
                    BreakScreenContent(
                        remainingSeconds = 15,
                        totalSeconds = 20,
                        strictMode = false,
                        onSkip = {},
                        onSnooze = {}
                    )
                }
            }
        }
        assertNotNull("ComposeView in portrait should not be null", composeView)
    }

    @Test
    fun testBreakScreenContentRendersInLandscape() {
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
                    BreakScreenContent(
                        remainingSeconds = 15,
                        totalSeconds = 20,
                        strictMode = true,
                        onSkip = {},
                        onSnooze = {}
                    )
                }
            }
        }
        assertNotNull("ComposeView in landscape should not be null", composeView)
    }
}
