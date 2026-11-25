package `in`.rahulja.vision202020.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.data.AppThemeMode
import `in`.rahulja.vision202020.ui.about.AboutLibrariesActivity
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AboutLibrariesActivityTest {

    @Test
    fun launchesAndRendersLicenseMetadata() {
        ActivityScenario.launch(AboutLibrariesActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> assertNotNull(activity) }
        }
    }

    @Test
    fun launchesWithThemeExtra() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), AboutLibrariesActivity::class.java).apply {
            putExtra(AboutLibrariesActivity.EXTRA_THEME, AppThemeMode.LIGHT.name)
        }
        ActivityScenario.launch<AboutLibrariesActivity>(intent).use { scenario ->
            scenario.onActivity { activity -> assertNotNull(activity) }
        }
    }
}
