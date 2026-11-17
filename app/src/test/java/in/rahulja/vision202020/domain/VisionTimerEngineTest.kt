package `in`.rahulja.vision202020.domain

import `in`.rahulja.vision202020.data.EnforcementMode
import `in`.rahulja.vision202020.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTimeSource(var currentMillis: Long = 1000000L) {
    fun advanceSeconds(seconds: Int) {
        currentMillis += seconds * 1000L
    }
    fun advanceMinutes(minutes: Int) {
        currentMillis += minutes * 60 * 1000L
    }
}

class VisionTimerEngineTest {

    private lateinit var fakeTime: FakeTimeSource
    private lateinit var defaultPrefs: UserPreferences

    @Before
    fun setup() {
        fakeTime = FakeTimeSource()
        defaultPrefs = UserPreferences(
            workDurationMinutes = 20,
            restDurationSeconds = 20,
            naturalBreakMinutes = 5,
            enforcementMode = EnforcementMode.BLOCKER_OVERLAY
        )
    }

    @Test
    fun testInitialStateIsIdle() {
        val engine = VisionTimerEngine(defaultPrefs, timeProvider = { fakeTime.currentMillis })
        assertEquals(TimerStatus.Idle, engine.state.value)
        assertFalse(engine.isSessionActive)
    }

    @Test
    fun testStartStartsWorkingState() {
        val engine = VisionTimerEngine(defaultPrefs, timeProvider = { fakeTime.currentMillis })
        engine.start()
        val state = engine.state.value
        assertTrue(state is TimerStatus.Working)
        state as TimerStatus.Working
        assertEquals(1200, state.remainingSeconds)
        assertEquals(0, state.elapsedSeconds)
        assertEquals(false, state.isScreenPaused)
        assertTrue(engine.isSessionActive)
    }

    @Test
    fun testTickingReachesBreak() {
        val shortPrefs = defaultPrefs.copy(workDurationMinutes = 1, restDurationSeconds = 20)
        val engine = VisionTimerEngine(shortPrefs, timeProvider = { fakeTime.currentMillis })
        engine.start()

        repeat(60) {
            fakeTime.advanceSeconds(1)
            engine.tickSecond()
        }

        val state = engine.state.value
        assertTrue("Expected RestBreak, got $state", state is TimerStatus.RestBreak)
        state as TimerStatus.RestBreak
        assertEquals(20, state.remainingSeconds)
        assertEquals(20, state.totalSeconds)
    }

    @Test
    fun testBreakCompletionAutoStartsNextWorkingSessionAndFiresCallback() {
        val shortPrefs = defaultPrefs.copy(workDurationMinutes = 20, restDurationSeconds = 20)
        val engine = VisionTimerEngine(shortPrefs, timeProvider = { fakeTime.currentMillis })
        var callbackFired = false
        engine.onBreakCompleted = { callbackFired = true }

        engine.start()
        engine.triggerBreak()

        engine.completeBreak()

        assertTrue("Break completed callback should have fired", callbackFired)
        val state = engine.state.value
        assertTrue("Expected next Working cycle, got $state", state is TimerStatus.Working)
        state as TimerStatus.Working
        assertEquals(1200, state.remainingSeconds)
        assertEquals(0, state.elapsedSeconds)
        assertFalse(state.isScreenPaused)
    }

    @Test
    fun testNaturalBreakResetAfterThreshold() {
        val engine = VisionTimerEngine(defaultPrefs, timeProvider = { fakeTime.currentMillis })
        engine.start()

        // Work for 10 minutes (600 seconds)
        repeat(600) {
            fakeTime.advanceSeconds(1)
            engine.tickSecond()
        }

        // Screen turned off
        engine.onScreenOff()
        val pausedState = engine.state.value as TimerStatus.Working
        assertTrue(pausedState.isScreenPaused)

        // Screen stays off for 6 minutes (exceeds 5m natural break threshold)
        fakeTime.advanceMinutes(6)

        // Screen turned on -> resets accumulator and auto-starts fresh countdown!
        engine.onScreenOn()
        val resumedState = engine.state.value as TimerStatus.Working
        assertEquals(false, resumedState.isScreenPaused)
        assertEquals(0, resumedState.elapsedSeconds)
        assertEquals(1200, resumedState.remainingSeconds)
        assertTrue(engine.isSessionActive)
    }

    @Test
    fun testScreenOffUnderThresholdDoesNotResetAccumulator() {
        val engine = VisionTimerEngine(defaultPrefs, timeProvider = { fakeTime.currentMillis })
        engine.start()

        // Work for 10 minutes (600 seconds)
        repeat(600) {
            fakeTime.advanceSeconds(1)
            engine.tickSecond()
        }

        // Screen off for only 2 minutes (under 5m threshold)
        engine.onScreenOff()
        fakeTime.advanceMinutes(2)
        engine.onScreenOn()

        val resumedState = engine.state.value as TimerStatus.Working
        assertEquals(false, resumedState.isScreenPaused)
        assertEquals(600, resumedState.elapsedSeconds)
        assertEquals(600, resumedState.remainingSeconds)
    }

    @Test
    fun testScreenOffDuringRestBreakAdvancesCountdown() {
        val shortPrefs = defaultPrefs.copy(workDurationMinutes = 1, restDurationSeconds = 20)
        val engine = VisionTimerEngine(shortPrefs, timeProvider = { fakeTime.currentMillis })
        engine.start()

        repeat(60) {
            fakeTime.advanceSeconds(1)
            engine.tickSecond()
        }

        assertTrue(engine.state.value is TimerStatus.RestBreak)

        // User locks screen to rest eyes
        engine.onScreenOff()
        // 20 seconds elapse while screen off
        fakeTime.advanceSeconds(20)
        engine.tickSecond()

        // Break finishes and next cycle begins automatically
        val nextState = engine.state.value
        assertTrue("Expected new Working cycle, got $nextState", nextState is TimerStatus.Working)
        assertTrue("Next cycle should remain paused while screen is off", (nextState as TimerStatus.Working).isScreenPaused)
    }

    @Test
    fun testManualStopCancelsSession() {
        val engine = VisionTimerEngine(defaultPrefs, timeProvider = { fakeTime.currentMillis })
        engine.start()
        assertTrue(engine.isSessionActive)

        engine.stop()
        assertFalse(engine.isSessionActive)
        assertEquals(TimerStatus.Idle, engine.state.value)
    }

    @Test
    fun testDurationReductionTriggersImmediateBreak() {
        val engine = VisionTimerEngine(defaultPrefs, timeProvider = { fakeTime.currentMillis })
        engine.start()

        // Elapsed 15 minutes
        repeat(900) {
            fakeTime.advanceSeconds(1)
            engine.tickSecond()
        }

        // User changes duration to 10 minutes in settings
        engine.updatePreferences(defaultPrefs.copy(workDurationMinutes = 10))

        assertTrue("Expected immediate RestBreak when elapsed exceeds new duration", engine.state.value is TimerStatus.RestBreak)
    }

    @Test
    fun testSnoozeTransitions() {
        val shortPrefs = defaultPrefs.copy(workDurationMinutes = 1, restDurationSeconds = 20)
        val engine = VisionTimerEngine(shortPrefs, timeProvider = { fakeTime.currentMillis })
        engine.start()

        repeat(60) {
            fakeTime.advanceSeconds(1)
            engine.tickSecond()
        }

        engine.snooze(durationSeconds = 300)
        val snoozeState = engine.state.value
        assertTrue(snoozeState is TimerStatus.Snoozed)
        assertEquals(300, (snoozeState as TimerStatus.Snoozed).remainingSeconds)

        repeat(300) {
            fakeTime.advanceSeconds(1)
            engine.tickSecond()
        }

        assertTrue("Expected RestBreak after snooze expires", engine.state.value is TimerStatus.RestBreak)
    }
}
