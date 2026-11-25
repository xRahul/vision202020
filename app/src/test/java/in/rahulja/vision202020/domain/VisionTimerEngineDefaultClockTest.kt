package `in`.rahulja.vision202020.domain

import android.os.SystemClock
import `in`.rahulja.vision202020.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/** Pins the default time provider to the monotonic clock; a wall-clock default would pass every fake-clock test. */
@RunWith(RobolectricTestRunner::class)
class VisionTimerEngineDefaultClockTest {

    @Test
    fun defaultProviderFollowsElapsedRealtime() {
        val engine = VisionTimerEngine(UserPreferences(workDurationMinutes = 20))
        engine.start()
        val start = engine.state.value as TimerStatus.Working

        ShadowSystemClock.advanceBy(Duration.ofSeconds(30))
        engine.tickSecond()

        val after = engine.state.value as TimerStatus.Working
        assertEquals(start.deadlineTimestampMs, after.deadlineTimestampMs)
        assertEquals(start.remainingSeconds - 30, after.remainingSeconds)
        assertEquals(SystemClock.elapsedRealtime() + after.remainingSeconds * 1000L, after.deadlineTimestampMs)
    }
}
