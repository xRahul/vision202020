package `in`.rahulja.vision202020.service

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.SessionStore
import `in`.rahulja.vision202020.data.dataStore
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

    private fun deliver(action: String, receiver: BootReceiver = BootReceiver()) {
        receiver.onReceive(context, Intent(action))
        // Deterministic: wait for the receiver's own work, then run what it posted to the main looper.
        runBlocking { receiver.workJob?.join() }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun setStartOnBoot(enabled: Boolean) {
        runBlocking {
            PreferencesRepositoryImpl(context.dataStore).updateStartOnBoot(enabled)
        }
    }

    @Test
    fun bootClearsSnapshot() {
        setStartOnBoot(false)
        setBootCount(2)
        runBlocking { store.save(working(bootCount = 2)) }

        deliver(Intent.ACTION_BOOT_COMPLETED)

        // Same boot count would normally load; only an explicit clear removes it.
        assertNull(runBlocking { store.load(currentBootCount = 2, nowElapsedMs = 0L) })
    }

    @Test
    fun packageReplacedAfterRebootDoesNotRestore() {
        setStartOnBoot(false)
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

    @Test
    fun bootWithStartOnBootStartsTheTimerAfterClearing() {
        setStartOnBoot(true)
        setBootCount(2)
        runBlocking { store.save(working(bootCount = 2)) }

        deliver(Intent.ACTION_BOOT_COMPLETED)

        assertNull(runBlocking { store.load(currentBootCount = 2, nowElapsedMs = 0L) })
        assertEquals(VisionTimerService.ACTION_START, shadowOf(context as Application).nextStartedService.action)
    }

    @Test
    fun bootWithoutStartOnBootStartsNothing() {
        setStartOnBoot(false)
        setBootCount(2)

        deliver(Intent.ACTION_BOOT_COMPLETED)

        assertNull(shadowOf(context as Application).peekNextStartedService())
    }

    @Test
    fun failingWorkDoesNotCrashTheProcess() {
        val crashes = mutableListOf<Throwable>()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> crashes += e }
        try {
            val receiver = object : BootReceiver() {
                override suspend fun handle(context: Context, action: String): Unit = throw java.io.IOException("datastore unavailable")
            }
            deliver(Intent.ACTION_BOOT_COMPLETED, receiver)
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }

        assertEquals(emptyList<Throwable>(), crashes)
    }
}
