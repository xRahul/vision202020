package `in`.rahulja.vision202020

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry

internal object SmokeSupport {
    /** Emulator UI and service start-up can be slow on CI, so appear/disappear waits get a generous bound. */
    const val TIMEOUT_MS = 15_000L

    /** Polls [condition] until it holds or [timeoutMs] passes; returns the final result. */
    fun waitFor(timeoutMs: Long = TIMEOUT_MS, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return true
            SystemClock.sleep(100)
        }
        return condition()
    }

    /** Runs a shell command as the shell user and blocks until it finishes by draining its output. */
    fun shell(command: String): String {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes().decodeToString() }
    }
}
