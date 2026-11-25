package `in`.rahulja.vision202020

import android.os.SystemClock

internal object SmokeSupport {
    const val TIMEOUT_MS = 5_000L

    /** Polls [condition] until it holds or [timeoutMs] passes; returns the final result. */
    fun waitFor(timeoutMs: Long = TIMEOUT_MS, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return true
            SystemClock.sleep(100)
        }
        return condition()
    }
}
