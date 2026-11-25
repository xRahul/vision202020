package `in`.rahulja.vision202020.domain

import android.os.SystemClock
import `in`.rahulja.vision202020.data.UserPreferences
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock

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

    /**
     * Robolectric couples the wall clock to the monotonic clock and only lets it move forward, so a
     * true wall-only jump cannot be simulated. What it can pin is the timebase: deadlines live on
     * the elapsedRealtime scale, far from the wall-clock epoch scale, so a wall-clock default fails.
     */
    @Test
    fun deadlinesLiveOnTheMonotonicTimebaseNotTheWallClock() {
        val engine = VisionTimerEngine(UserPreferences(workDurationMinutes = 20))
        engine.start()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(30))
        engine.tickSecond()

        val state = engine.state.value as TimerStatus.Working
        val monotonicRemainingMs = state.deadlineTimestampMs - SystemClock.elapsedRealtime()
        assertEquals(1170, state.remainingSeconds)
        assertEquals(1170_000L, monotonicRemainingMs)
        assertTrue(
            "deadline must not be a wall-clock timestamp",
            kotlin.math.abs(state.deadlineTimestampMs - System.currentTimeMillis()) > DAY_MS
        )
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
