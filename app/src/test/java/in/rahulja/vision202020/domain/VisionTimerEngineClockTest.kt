package `in`.rahulja.vision202020.domain

import `in`.rahulja.vision202020.data.EnforcementMode
import `in`.rahulja.vision202020.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Clock-based behaviour: these tests deliberately never tick while time passes, which is
 * exactly what happens during Doze, main-thread jank or process death.
 */
class VisionTimerEngineClockTest {

    private lateinit var fakeTime: FakeTimeSource
    private lateinit var prefs: UserPreferences

    @Before
    fun setup() {
        fakeTime = FakeTimeSource()
        prefs = UserPreferences(
            workDurationMinutes = 20,
            restDurationSeconds = 20,
            naturalBreakMinutes = 5,
            enforcementMode = EnforcementMode.BLOCKER_OVERLAY
        )
    }

    private fun engine() = VisionTimerEngine(prefs, timeProvider = { fakeTime.currentMillis })

    @Test
    fun `missed ticks do not stretch the work timer`() {
        val engine = engine()
        engine.start()

        fakeTime.advanceMinutes(20)
        engine.tickSecond()

        assertTrue("Expected RestBreak, got ${engine.state.value}", engine.state.value is TimerStatus.RestBreak)
    }

    @Test
    fun `manual pause survives screen off and on`() {
        val engine = engine()
        engine.start()
        engine.pause()

        engine.onScreenOff()
        fakeTime.advanceMinutes(2)
        engine.onScreenOn()

        val state = engine.state.value as TimerStatus.Working
        assertEquals(PauseReason.MANUAL, state.pauseReason)
        assertEquals(1200, state.remainingSeconds)
    }

    @Test
    fun `short screen off keeps exact remaining time`() {
        val engine = engine()
        engine.start()

        fakeTime.advanceSeconds(10)
        engine.tickSecond()
        engine.onScreenOff()
        fakeTime.advanceSeconds(3)
        engine.onScreenOn()

        val state = engine.state.value as TimerStatus.Working
        assertEquals(PauseReason.NONE, state.pauseReason)
        assertEquals(1190, state.remainingSeconds)
        assertEquals(10, state.elapsedSeconds)
    }

    @Test
    fun `manual pause survives a natural break reset`() {
        val engine = engine()
        engine.start()
        fakeTime.advanceSeconds(100)
        engine.tickSecond()
        engine.pause()

        engine.onScreenOff()
        fakeTime.advanceMinutes(10)
        engine.onScreenOn()

        val state = engine.state.value as TimerStatus.Working
        assertEquals(PauseReason.MANUAL, state.pauseReason)
        assertEquals(1100, state.remainingSeconds)
    }

    @Test
    fun `long screen off resets to a fresh cycle`() {
        val engine = engine()
        engine.start()
        fakeTime.advanceMinutes(12)
        engine.tickSecond()

        engine.onScreenOff()
        fakeTime.advanceMinutes(6)
        engine.onScreenOn()

        val state = engine.state.value as TimerStatus.Working
        assertEquals(1200, state.remainingSeconds)
        assertEquals(0, state.elapsedSeconds)
        assertFalse(state.isScreenPaused)
    }

    @Test
    fun `restore continues from persisted deadline`() {
        val first = engine()
        first.start()
        fakeTime.advanceSeconds(100)
        first.tickSecond()
        val snapshot = SessionSnapshot(first.state.value, first.isSessionActive, 1, first.screenOffAtMs)

        fakeTime.advanceSeconds(1)
        val restored = VisionTimerEngine(prefs, timeProvider = { fakeTime.currentMillis })
        restored.restore(snapshot)
        restored.tickSecond()

        val state = restored.state.value as TimerStatus.Working
        assertEquals(1099, state.remainingSeconds)
        assertTrue(restored.isSessionActive)
    }

    @Test
    fun `restored expired session triggers break on first tick`() {
        val first = engine()
        first.start()
        val snapshot = SessionSnapshot(first.state.value, first.isSessionActive, 1, first.screenOffAtMs)

        fakeTime.advanceMinutes(21)
        val restored = VisionTimerEngine(prefs, timeProvider = { fakeTime.currentMillis })
        restored.restore(snapshot)
        restored.tickSecond()

        assertTrue(restored.state.value is TimerStatus.RestBreak)
    }

    @Test
    fun `shortening rest duration ends the break immediately`() {
        val engine = engine()
        engine.start()
        engine.triggerBreak()

        fakeTime.advanceSeconds(15)
        engine.updatePreferences(prefs.copy(restDurationSeconds = 10))

        assertTrue("Expected break to be over, got ${engine.state.value}", engine.state.value is TimerStatus.Working)
    }

    @Test
    fun `lengthening rest duration extends the running break`() {
        val engine = engine()
        engine.start()
        engine.triggerBreak()

        fakeTime.advanceSeconds(15)
        engine.updatePreferences(prefs.copy(restDurationSeconds = 40))

        val state = engine.state.value as TimerStatus.RestBreak
        assertEquals(40, state.totalSeconds)
        assertEquals(25, state.remainingSeconds)
    }

    @Test
    fun `snooze expires by wall clock without ticks`() {
        val engine = engine()
        engine.start()
        engine.triggerBreak()
        engine.snooze(durationSeconds = 300)

        fakeTime.advanceSeconds(299)
        engine.tickSecond()
        assertTrue(engine.state.value is TimerStatus.Snoozed)
        assertEquals(1, (engine.state.value as TimerStatus.Snoozed).remainingSeconds)

        fakeTime.advanceSeconds(2)
        engine.tickSecond()
        assertTrue(engine.state.value is TimerStatus.RestBreak)
    }

    @Test
    fun `unrelated preference change does not move the deadline`() {
        val engine = engine()
        engine.start()
        fakeTime.advanceSeconds(30)
        engine.tickSecond()

        val before = engine.state.value as TimerStatus.Working
        engine.updatePreferences(prefs.copy(soundEnabled = false, appTheme = `in`.rahulja.vision202020.data.AppThemeMode.LIGHT))
        val after = engine.state.value as TimerStatus.Working

        assertEquals(before.deadlineTimestampMs, after.deadlineTimestampMs)
        assertEquals(before.remainingSeconds, after.remainingSeconds)
    }

    @Test
    fun `break actions are ignored without an active session`() {
        val engine = engine()

        engine.completeBreak()
        engine.triggerBreak()
        engine.snooze(60)

        assertEquals(TimerStatus.Idle, engine.state.value)
        assertFalse(engine.isSessionActive)
    }

    @Test
    fun `reset restarts the cycle without ending the session`() {
        val engine = engine()
        engine.start()
        fakeTime.advanceMinutes(5)
        engine.tickSecond()

        engine.reset()

        val state = engine.state.value as TimerStatus.Working
        assertEquals(1200, state.remainingSeconds)
        assertTrue(engine.isSessionActive)
    }

    @Test
    fun `restored rest break clears stale screen off time when the screen is on`() {
        val first = engine()
        first.start()
        first.triggerBreak()
        val snapshot = SessionSnapshot(first.state.value, true, 1, screenOffAtMs = fakeTime.currentMillis - 1000L)
        val restored = engine()
        restored.restore(snapshot)

        // Mirrors the service: the screen is interactive, so it reports screen-on after restore.
        restored.onScreenOn()
        assertEquals(0L, restored.screenOffAtMs)
        fakeTime.advanceSeconds(21)
        restored.tickSecond()

        val state = restored.state.value as TimerStatus.Working
        assertFalse(state.isScreenPaused)
    }

    @Test
    fun `duplicate screen off keeps the first off time`() {
        val engine = engine()
        engine.start()
        engine.onScreenOff()
        val first = engine.screenOffAtMs
        fakeTime.advanceMinutes(6)

        engine.onScreenOff()
        assertEquals(first, engine.screenOffAtMs)
        engine.onScreenOn()

        val state = engine.state.value as TimerStatus.Working
        assertEquals("natural break still counted", 1200, state.remainingSeconds)
        assertFalse(state.isScreenPaused)
    }

    @Test
    fun `restore while screen off keeps the restored off time`() {
        val first = engine()
        first.start()
        first.onScreenOff()
        val snapshot = SessionSnapshot(first.state.value, true, 1, first.screenOffAtMs)
        fakeTime.advanceMinutes(6)

        val restored = engine()
        restored.restore(snapshot)
        restored.onScreenOff()

        assertEquals(snapshot.screenOffAtMs, restored.screenOffAtMs)
    }
}
