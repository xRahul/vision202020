package `in`.rahulja.vision202020.service

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.data.SessionStore
import `in`.rahulja.vision202020.data.sessionDataStore
import `in`.rahulja.vision202020.domain.PauseReason
import `in`.rahulja.vision202020.domain.SessionSnapshot
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.domain.VisionTimerStateHolder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class VisionTimerServiceRestoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = SessionStore(context.sessionDataStore)

    @Before
    fun setUp() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.BOOT_COUNT, 1)
        runBlocking { store.clear() }
        VisionTimerStateHolder.setState(TimerStatus.Idle)
    }

    private fun restoreIntent() = Intent(context, VisionTimerService::class.java)
        .setAction(VisionTimerService.ACTION_RESTORE)

    /** DataStore I/O runs off the main thread, so pump the looper until the condition holds. */
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
    fun restoreActionResumesSavedSession() {
        runBlocking {
            store.save(
                SessionSnapshot(
                    status = TimerStatus.Working(
                        remainingSeconds = 600,
                        elapsedSeconds = 600,
                        totalSeconds = 1200,
                        pauseReason = PauseReason.MANUAL,
                        remainingAtPauseMs = 600_000L
                    ),
                    isSessionActive = true,
                    bootCount = 1,
                    screenOffAtMs = 0L
                )
            )
        }
        val controller = Robolectric.buildService(VisionTimerService::class.java, restoreIntent())
        val service = controller.get()
        // Hold the restore open: the service must stay alive while the engine is still Idle.
        val gate = CompletableDeferred<Unit>()
        service.beforeRestoreLoad = { gate.await() }
        controller.create().startCommand(0, 1)

        shadowOf(Looper.getMainLooper()).idle()
        assertFalse("RESTORE must not stop the service before restore finishes", shadowOf(service).isStoppedBySelf)
        gate.complete(Unit)
        val restored = awaitCondition { VisionTimerStateHolder.state.value is TimerStatus.Working }

        assertTrue("session should be restored", restored)
        assertFalse(shadowOf(service).isStoppedBySelf)
    }

    @Test
    fun restoreWithNothingToResumeStopsAndRemovesForeground() {
        val controller = Robolectric.buildService(VisionTimerService::class.java, restoreIntent())
        val service = controller.create().startCommand(0, 1).get()

        val stopped = awaitCondition { shadowOf(service).isStoppedBySelf }

        assertTrue("service should stop itself", stopped)
        assertTrue(shadowOf(service).notificationShouldRemoved)
    }
}
