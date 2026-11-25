package `in`.rahulja.vision202020.ui

import android.provider.Settings
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.ui.breakui.BreakActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BreakActivityTest {

    @Before
    fun disableAnimations() {
        // BreakScreenContent runs an infinite "breathing" animation. Under Robolectric's paused
        // looper it never lets idle() return, so the launch spins for minutes. Compose reads
        // this setting and finishes infinite animations immediately when the scale is 0.
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f
        )
    }

    @Test
    fun testKeepScreenOnFlagIsSet() {
        ActivityScenario.launch(BreakActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                val flags = activity.window.attributes.flags
                assertTrue(
                    "BreakActivity must set FLAG_KEEP_SCREEN_ON on API 27+ to prevent lockscreen sleep",
                    (flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
                )
            }
        }
    }
}
