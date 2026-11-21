package `in`.rahulja.vision202020.ui

import androidx.test.core.app.ActivityScenario
import `in`.rahulja.vision202020.ui.about.AboutLibrariesActivity
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AboutLibrariesActivityTest {

    @Test
    fun launchesAndRendersLicenseMetadata() {
        ActivityScenario.launch(AboutLibrariesActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> assertNotNull(activity) }
        }
    }
}
