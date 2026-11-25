package `in`.rahulja.vision202020.ui

import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import `in`.rahulja.vision202020.ui.breakui.BreakActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BreakActivityTest {

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
