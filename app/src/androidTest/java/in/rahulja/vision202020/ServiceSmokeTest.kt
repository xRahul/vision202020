package `in`.rahulja.vision202020

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import `in`.rahulja.vision202020.service.NotificationHelper
import `in`.rahulja.vision202020.service.VisionTimerService
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ServiceSmokeTest {

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun timerNotificationPosted() = manager.activeNotifications.any { it.id == NotificationHelper.NOTIFICATION_ID_TIMER }

    @After
    fun tearDown() {
        VisionTimerService.stop(context)
        SmokeSupport.waitFor { !timerNotificationPosted() }
    }

    @Test
    fun startPostsForegroundNotificationAndStopRemovesIt() {
        VisionTimerService.start(context)
        assertTrue("timer notification should appear", SmokeSupport.waitFor { timerNotificationPosted() })

        VisionTimerService.stop(context)
        assertTrue("timer notification should be removed", SmokeSupport.waitFor { !timerNotificationPosted() })
    }
}
