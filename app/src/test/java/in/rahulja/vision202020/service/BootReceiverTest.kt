package `in`.rahulja.vision202020.service

import android.app.Application
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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BootReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = SessionStore(context.sessionDataStore)

    @Before
    fun setUp() {
        runBlocking { store.clear() }
    }

    private fun setBootCount(count: Int) {
        Settings.Global.putInt(context.contentResolver, Settings.Global.BOOT_COUNT, count)
    }

    private fun working(bootCount: Int) = SessionSnapshot(
        status = TimerStatus.Working(
            remainingSeconds = 600,
            elapsedSeconds = 600,
            totalSeconds = 1200,
            pauseReason = PauseReason.NONE,
            deadlineTimestampMs = 600_000L
        ),
        isSessionActive = true,
        bootCount = bootCount,
        screenOffAtMs = 0L
    )

    private fun deliver(action: String) {
        BootReceiver().onReceive(context, Intent(action))
        waitForReceiver()
    }

    /** The receiver works on Dispatchers.IO; poll until a service start shows up or time runs out. */
    private fun waitForReceiver() {
        val deadline = System.currentTimeMillis() + 3_000
        val app = context as Application
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (shadowOf(app).peekNextStartedService() != null) return
            Thread.sleep(25)
        }
    }

    @Test
    fun bootClearsSnapshot() {
        setBootCount(2)
        runBlocking { store.save(working(bootCount = 2)) }

        deliver(Intent.ACTION_BOOT_COMPLETED)

        // Same boot count would normally load; only an explicit clear removes it.
        assertNull(runBlocking { store.load(currentBootCount = 2, nowElapsedMs = 0L) })
    }

    @Test
    fun packageReplacedAfterRebootDoesNotRestore() {
        runBlocking { store.save(working(bootCount = 1)) }
        setBootCount(2)

        deliver(Intent.ACTION_MY_PACKAGE_REPLACED)

        assertNull(shadowOf(context as Application).peekNextStartedService())
    }

    @Test
    fun packageReplacedSameBootRestores() {
        setBootCount(1)
        runBlocking { store.save(working(bootCount = 1)) }

        deliver(Intent.ACTION_MY_PACKAGE_REPLACED)

        val started = shadowOf(context as Application).nextStartedService
        assertNotNull(started)
        assertEquals(VisionTimerService.ACTION_RESTORE, started.action)
    }
}
