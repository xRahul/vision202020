package `in`.rahulja.vision202020

import android.Manifest
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import `in`.rahulja.vision202020.data.EnforcementMode
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.dataStore
import `in`.rahulja.vision202020.service.VisionTimerService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OverlaySmokeTest {

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val title by lazy { context.getString(R.string.break_rest_eyes) }

    @Before
    fun setUp() {
        // Debug builds carry an applicationIdSuffix, so use the runtime package name.
        instrumentation.uiAutomation
            .executeShellCommand("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
            .close()
        runBlocking {
            PreferencesRepositoryImpl(context.dataStore).updateEnforcementMode(EnforcementMode.BLOCKER_OVERLAY)
        }
        device.wakeUp()
    }

    @After
    fun tearDown() {
        VisionTimerService.stop(context)
    }

    @Test
    fun breakShowsOverlay() {
        VisionTimerService.start(context)
        VisionTimerService.triggerBreak(context)
        assertTrue("break overlay should appear", device.wait(Until.hasObject(By.text(title)), SmokeSupport.TIMEOUT_MS))

        VisionTimerService.completeBreak(context)
        assertTrue("break overlay should disappear", device.wait(Until.gone(By.text(title)), SmokeSupport.TIMEOUT_MS))
    }
}
