package `in`.rahulja.vision202020.service

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.data.EnforcementMode
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.SessionStore
import `in`.rahulja.vision202020.data.dataStore
import `in`.rahulja.vision202020.data.sessionDataStore
import `in`.rahulja.vision202020.domain.SessionSnapshot
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.domain.VisionTimerStateHolder
import java.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
class VisionTimerServiceBreakDeliveryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = SessionStore(context.sessionDataStore)

    @Before
    fun setUp() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.BOOT_COUNT, 1)
        runBlocking { store.clear() }
        VisionTimerStateHolder.setState(TimerStatus.Idle)
        ShadowSettings.setCanDrawOverlays(true)
    }

    private fun intent(action: String) = Intent(context, VisionTimerService::class.java).setAction(action)

    private fun notificationManager() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun breakNotification() = shadowOf(notificationManager()).getNotification(NotificationHelper.NOTIFICATION_ID_BREAK)

    private fun awaitCondition(condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return true
            Thread.sleep(25)
        }
        return condition()
    }

    private fun lockKeyguard(locked: Boolean) {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        shadowOf(keyguard).setKeyguardLocked(locked)
    }

    private fun startBreak(locked: Boolean): VisionTimerService {
        lockKeyguard(locked)
        runBlocking {
            PreferencesRepositoryImpl(context.dataStore).updateEnforcementMode(EnforcementMode.BLOCKER_OVERLAY)
            store.save(
                SessionSnapshot(
                    status = TimerStatus.RestBreak(
                        remainingSeconds = 20,
                        totalSeconds = 20,
                        breakStartedTimestampMs = SystemClock.elapsedRealtime()
                    ),
                    isSessionActive = true,
                    bootCount = 1,
                    screenOffAtMs = 0L
                )
            )
        }
        val controller = Robolectric.buildService(VisionTimerService::class.java, intent(VisionTimerService.ACTION_RESTORE))
        controller.create().startCommand(0, 1)
        assertTrue(awaitCondition { VisionTimerStateHolder.state.value is TimerStatus.RestBreak })
        shadowOf(Looper.getMainLooper()).idle()
        return controller.get()
    }

    @Test
    fun lockedKeyguardUsesFullScreenNotification() {
        val service = startBreak(locked = true)

        assertFalse("no overlay behind the keyguard", service.overlayManager.isShowing)
        assertNotNull("break notification must be posted", breakNotification())
        assertNotNull(breakNotification().fullScreenIntent)
    }

    @Test
    fun userPresentSwapsNotificationForOverlay() {
        val service = startBreak(locked = true)
        assertNotNull(breakNotification())

        lockKeyguard(false)
        // Called directly: once the overlay's animation runs, idling the looper races the robolectric clock past the break.
        service.onUserPresent()

        assertTrue(service.overlayManager.isShowing)
        assertNull("alert notification must be withdrawn", breakNotification())
    }

    @Test
    fun breakAlertPostedOncePerBreak() {
        startBreak(locked = true)
        notificationManager().cancel(NotificationHelper.NOTIFICATION_ID_BREAK)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))

        assertNull("ticks must not re-post the break alert", breakNotification())
    }
}
