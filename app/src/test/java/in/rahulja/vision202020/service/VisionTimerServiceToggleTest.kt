package `in`.rahulja.vision202020.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.SessionStore
import `in`.rahulja.vision202020.data.dataStore
import `in`.rahulja.vision202020.data.sessionDataStore
import `in`.rahulja.vision202020.domain.PauseReason
import `in`.rahulja.vision202020.domain.SessionSnapshot
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.domain.VisionTimerStateHolder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class VisionTimerServiceToggleTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = SessionStore(context.sessionDataStore)

    @Before
    fun setUp() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.BOOT_COUNT, 1)
        runBlocking { store.clear() }
        VisionTimerStateHolder.setState(TimerStatus.Idle)
    }

    private fun intent(action: String) = Intent(context, VisionTimerService::class.java).setAction(action)

    private fun awaitCondition(condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return true
            Thread.sleep(25)
        }
        return condition()
    }

    @Test
    fun toggleDuringRestoreActsOnRestoredSession() {
        runBlocking {
            store.save(
                SessionSnapshot(
                    status = TimerStatus.Working(
                        remainingSeconds = 600,
                        elapsedSeconds = 600,
                        totalSeconds = 1200,
                        pauseReason = PauseReason.NONE,
                        deadlineTimestampMs = SystemClock.elapsedRealtime() + 600_000L
                    ),
                    isSessionActive = true,
                    bootCount = 1,
                    screenOffAtMs = 0L
                )
            )
        }
        val controller = Robolectric.buildService(VisionTimerService::class.java, intent(VisionTimerService.ACTION_RESTORE))
        val service = controller.get()
        // Hold the restore open so the toggle arrives while the engine is still Idle.
        val gate = CompletableDeferred<Unit>()
        service.beforeRestoreLoad = { gate.await() }
        controller.create().startCommand(0, 1)
        controller.withIntent(intent(VisionTimerService.ACTION_TOGGLE)).startCommand(0, 2)

        shadowOf(Looper.getMainLooper()).idle()
        assertFalse("toggle must wait for restore", shadowOf(service).isStoppedBySelf)
        assertTrue(VisionTimerStateHolder.state.value is TimerStatus.Idle)
        gate.complete(Unit)

        val paused = awaitCondition {
            (VisionTimerStateHolder.state.value as? TimerStatus.Working)?.pauseReason == PauseReason.MANUAL
        }

        assertTrue("restored session should be paused, not restarted", paused)
        val working = VisionTimerStateHolder.state.value as TimerStatus.Working
        assertTrue("must keep restored cycle, not a fresh one", working.remainingAtPauseMs in 1L..600_000L)
        assertFalse(shadowOf(service).isStoppedBySelf)
    }

    @Test
    fun toggleWithNoSnapshotStarts() {
        val controller = Robolectric.buildService(VisionTimerService::class.java, intent(VisionTimerService.ACTION_RESTORE))
        val service = controller.get()
        // The toggle must land while restore is still loading, as on a cold tile tap.
        val gate = CompletableDeferred<Unit>()
        service.beforeRestoreLoad = { gate.await() }
        controller.create().startCommand(0, 1)
        controller.withIntent(intent(VisionTimerService.ACTION_TOGGLE)).startCommand(0, 2)
        gate.complete(Unit)

        val started = awaitCondition { VisionTimerStateHolder.state.value is TimerStatus.Working }

        assertTrue("toggle on empty store should start a session", started)
        assertEquals(PauseReason.NONE, (VisionTimerStateHolder.state.value as TimerStatus.Working).pauseReason)
        assertFalse(shadowOf(service).isStoppedBySelf)
    }

    @Test
    fun sendReturnsFalseWhenForegroundStartDenied() {
        val denied = mockk<Context>(relaxed = true)
        every { denied.packageName } returns context.packageName
        every { denied.startForegroundService(any()) } throws IllegalStateException("not allowed")

        assertFalse(VisionTimerService.toggle(denied))
        assertFalse(VisionTimerService.start(denied))
    }

    @Test
    fun sendReturnsTrueWhenStarted() {
        assertTrue(VisionTimerService.toggle(context))
    }

    /** Restores a live break with strict mode on and returns the running service. */
    private fun startStrictBreak(strict: Boolean = true): VisionTimerService {
        runBlocking {
            PreferencesRepositoryImpl(context.dataStore).updateStrictMode(strict)
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
        // Restore applies the stored preferences after loading the snapshot.
        shadowOf(Looper.getMainLooper()).idle()
        activeController = controller
        return controller.get()
    }

    private var activeController: org.robolectric.android.controller.ServiceController<VisionTimerService>? = null

    private fun notificationManager() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun breakNotificationPosted() =
        shadowOf(notificationManager()).getNotification(NotificationHelper.NOTIFICATION_ID_BREAK) != null

    @Test
    fun strictToggleDuringBreakDoesNotComplete() {
        startStrictBreak()
        notificationManager().cancel(NotificationHelper.NOTIFICATION_ID_BREAK)

        activeController!!.withIntent(intent(VisionTimerService.ACTION_TOGGLE)).startCommand(0, 2)

        assertTrue("break notification must be re-posted", awaitCondition { breakNotificationPosted() })
        assertTrue(VisionTimerStateHolder.state.value is TimerStatus.RestBreak)
    }

    @Test
    fun strictCompleteWithoutUiExtraIsIgnored() {
        startStrictBreak()
        notificationManager().cancel(NotificationHelper.NOTIFICATION_ID_BREAK)

        activeController!!.withIntent(intent(VisionTimerService.ACTION_COMPLETE_BREAK)).startCommand(0, 2)

        assertTrue("break notification must be re-posted", awaitCondition { breakNotificationPosted() })
        assertTrue(VisionTimerStateHolder.state.value is TimerStatus.RestBreak)
    }

    @Test
    fun strictCompleteWithUiExtraCompletes() {
        startStrictBreak()

        val completion = intent(VisionTimerService.ACTION_COMPLETE_BREAK)
            .putExtra(VisionTimerService.EXTRA_FROM_BREAK_UI, true)
        activeController!!.withIntent(completion).startCommand(0, 2)

        assertTrue(awaitCondition { VisionTimerStateHolder.state.value is TimerStatus.Working })
    }

    @Test
    fun nonStrictCompleteWithoutUiExtraCompletes() {
        startStrictBreak(strict = false)

        activeController!!.withIntent(intent(VisionTimerService.ACTION_COMPLETE_BREAK)).startCommand(0, 2)

        assertTrue(awaitCondition { VisionTimerStateHolder.state.value is TimerStatus.Working })
    }
}
