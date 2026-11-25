package `in`.rahulja.vision202020

import android.Manifest
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import `in`.rahulja.vision202020.data.EnforcementMode
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.dataStore
import `in`.rahulja.vision202020.service.NotificationHelper
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
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val title by lazy { context.getString(R.string.break_rest_eyes) }

    private fun timerNotificationPosted() =
        notificationManager.activeNotifications.any { it.id == NotificationHelper.NOTIFICATION_ID_TIMER }

    @Before
    fun setUp() {
        // Debug builds carry an applicationIdSuffix, so use the runtime package name.
        SmokeSupport.shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
        assertTrue(
            "SYSTEM_ALERT_WINDOW was not granted after appops allow",
            SmokeSupport.waitFor { Settings.canDrawOverlays(context) }
        )
        runBlocking {
            PreferencesRepositoryImpl(context.dataStore).updateEnforcementMode(EnforcementMode.BLOCKER_OVERLAY)
        }
        // The service skips the overlay while the keyguard is up, so the screen must be awake and unlocked.
        device.wakeUp()
        SmokeSupport.shell("wm dismiss-keyguard")
        assertTrue(
            "keyguard still locked; the service skips the overlay while it is shown",
            SmokeSupport.waitFor { !keyguardManager.isKeyguardLocked }
        )
    }

    @After
    fun tearDown() {
        VisionTimerService.stop(context)
        SmokeSupport.waitFor { !timerNotificationPosted() }
        SmokeSupport.shell("appops reset ${context.packageName}")
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
