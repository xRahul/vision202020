package `in`.rahulja.vision202020.domain

import `in`.rahulja.vision202020.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Clock-based 20-20-20 state machine.
 *
 * Every running state stores an absolute deadline, so a late or missed tick (Doze,
 * main-thread jank, process death) cannot stretch the session. Ticks only translate
 * the deadline into display values.
 */
class VisionTimerEngine(private var prefs: UserPreferences, private val timeProvider: () -> Long = { System.currentTimeMillis() }) {
    private val _state = MutableStateFlow<TimerStatus>(TimerStatus.Idle)
    val state: StateFlow<TimerStatus> = _state.asStateFlow()

    private var _isSessionActive: Boolean = false
    val isSessionActive: Boolean get() = _isSessionActive

    var onBreakCompleted: (() -> Unit)? = null

    private var screenOffTimestampMs: Long = 0L

    fun start() {
        _isSessionActive = true
        screenOffTimestampMs = 0L
        _state.value = freshWorking(PauseReason.NONE)
    }

    /** Restart the current cycle without ending the session. */
    fun reset() {
        if (!_isSessionActive) return
        _state.value = freshWorking(PauseReason.NONE)
    }

    fun restore(snapshot: SessionSnapshot) {
        _isSessionActive = snapshot.isSessionActive
        screenOffTimestampMs = 0L
        _state.value = snapshot.status
    }

    fun pause() {
        val current = _state.value
        if (current is TimerStatus.Working && !current.isScreenPaused) {
            _state.value = current.copy(
                pauseReason = PauseReason.MANUAL,
                remainingAtPauseMs = (current.deadlineTimestampMs - timeProvider()).coerceAtLeast(0L)
            )
        }
    }

    fun resume() {
        val current = _state.value
        if (current is TimerStatus.Working && current.isScreenPaused) {
            val remainingMs = current.remainingAtPauseMs.coerceAtLeast(0L)
            val remainingSeconds = ceilSeconds(remainingMs)
            _state.value = current.copy(
                pauseReason = PauseReason.NONE,
                deadlineTimestampMs = timeProvider() + remainingMs,
                remainingAtPauseMs = 0L,
                remainingSeconds = remainingSeconds,
                elapsedSeconds = (current.totalSeconds - remainingSeconds).coerceAtLeast(0)
            )
        }
    }

    fun stop() {
        _isSessionActive = false
        screenOffTimestampMs = 0L
        _state.value = TimerStatus.Idle
    }

    fun updatePreferences(newPrefs: UserPreferences) {
        val workDurationChanged = newPrefs.workDurationMinutes != prefs.workDurationMinutes
        val restDurationChanged = newPrefs.restDurationSeconds != prefs.restDurationSeconds
        prefs = newPrefs

        when (val current = _state.value) {
            is TimerStatus.Working -> {
                if (!workDurationChanged) return
                val newTotalSec = workTotalSeconds()
                // Derive elapsed from the deadline, never from a possibly stale tick count.
                val remainingMsNow = if (current.isScreenPaused) {
                    current.remainingAtPauseMs
                } else {
                    (current.deadlineTimestampMs - timeProvider()).coerceAtLeast(0L)
                }
                val elapsedSec = (current.totalSeconds - ceilSeconds(remainingMsNow)).coerceAtLeast(0)
                if (elapsedSec >= newTotalSec) {
                    triggerBreak()
                } else {
                    val newRemaining = newTotalSec - elapsedSec
                    _state.value = if (current.isScreenPaused) {
                        current.copy(
                            remainingSeconds = newRemaining,
                            elapsedSeconds = elapsedSec,
                            totalSeconds = newTotalSec,
                            remainingAtPauseMs = newRemaining * 1000L
                        )
                    } else {
                        current.copy(
                            remainingSeconds = newRemaining,
                            elapsedSeconds = elapsedSec,
                            totalSeconds = newTotalSec,
                            deadlineTimestampMs = timeProvider() + newRemaining * 1000L
                        )
                    }
                }
            }

            is TimerStatus.RestBreak -> {
                if (!restDurationChanged) return
                // Keep the break anchored to its start so shortening it can end it now.
                val newTotalSec = newPrefs.restDurationSeconds
                val elapsedSec = ((timeProvider() - current.breakStartedTimestampMs) / 1000L).toInt()
                val newRemaining = (newTotalSec - elapsedSec).coerceAtLeast(0)
                if (newRemaining <= 0) {
                    completeBreak()
                } else {
                    _state.value = current.copy(
                        remainingSeconds = newRemaining,
                        totalSeconds = newTotalSec
                    )
                }
            }

            else -> Unit
        }
    }

    fun onScreenOff() {
        screenOffTimestampMs = timeProvider()
        val current = _state.value
        if (current is TimerStatus.Working && !current.isScreenPaused) {
            _state.value = current.copy(
                pauseReason = PauseReason.SCREEN_OFF,
                remainingAtPauseMs = (current.deadlineTimestampMs - timeProvider()).coerceAtLeast(0L)
            )
        }
    }

    fun onScreenOn() {
        val wasOff = screenOffTimestampMs > 0L
        val offDurationMs = if (wasOff) timeProvider() - screenOffTimestampMs else 0L
        val naturalBreakThresholdMs = prefs.naturalBreakMinutes * 60 * 1000L
        screenOffTimestampMs = 0L

        if (!_isSessionActive) return

        when (val current = _state.value) {
            is TimerStatus.RestBreak, is TimerStatus.Snoozed -> {
                // Break/snooze countdown advances while the screen is off.
                tickSecond()
            }

            is TimerStatus.Working -> {
                if (wasOff && offDurationMs >= naturalBreakThresholdMs) {
                    // A natural break restarts the cycle, but an explicit user pause keeps
                    // its exact position: only screen-off pausing is auto-managed.
                    if (current.pauseReason != PauseReason.MANUAL) {
                        _state.value = freshWorking(PauseReason.NONE)
                    }
                } else if (current.pauseReason == PauseReason.SCREEN_OFF) {
                    resume()
                }
                // Manual pause is preserved: only the user can clear it.
            }

            is TimerStatus.Idle -> {
                _state.value = freshWorking(PauseReason.NONE)
            }
        }
    }

    fun snooze(durationSeconds: Int = prefs.snoozeMinutes * 60) {
        if (!_isSessionActive) return
        val seconds = durationSeconds.coerceAtLeast(1)
        _state.value = TimerStatus.Snoozed(
            remainingSeconds = seconds,
            totalSeconds = seconds,
            deadlineTimestampMs = timeProvider() + seconds * 1000L
        )
    }

    fun completeBreak() {
        // Stale notification actions must not start a session out of nothing.
        if (!_isSessionActive) return
        val isScreenCurrentlyOff = screenOffTimestampMs > 0L
        _state.value = freshWorking(
            if (isScreenCurrentlyOff) PauseReason.SCREEN_OFF else PauseReason.NONE
        )
        onBreakCompleted?.invoke()
    }

    fun triggerBreak() {
        if (!_isSessionActive) return
        val seconds = prefs.restDurationSeconds
        _state.value = TimerStatus.RestBreak(
            remainingSeconds = seconds,
            totalSeconds = seconds,
            breakStartedTimestampMs = timeProvider()
        )
    }

    fun tickSecond() {
        val now = timeProvider()
        when (val current = _state.value) {
            is TimerStatus.Working -> {
                if (current.pauseReason != PauseReason.NONE) return
                val remainingMs = current.deadlineTimestampMs - now
                if (remainingMs <= 0L) {
                    triggerBreak()
                } else {
                    val remainingSeconds = ceilSeconds(remainingMs)
                    val elapsedSeconds = (current.totalSeconds - remainingSeconds).coerceAtLeast(0)
                    if (remainingSeconds != current.remainingSeconds || elapsedSeconds != current.elapsedSeconds) {
                        _state.value = current.copy(
                            remainingSeconds = remainingSeconds,
                            elapsedSeconds = elapsedSeconds
                        )
                    }
                }
            }

            is TimerStatus.RestBreak -> {
                val elapsedSec = ((now - current.breakStartedTimestampMs) / 1000L).toInt()
                val newRemaining = (current.totalSeconds - elapsedSec).coerceAtLeast(0)
                if (newRemaining <= 0) {
                    completeBreak()
                } else if (newRemaining != current.remainingSeconds) {
                    _state.value = current.copy(remainingSeconds = newRemaining)
                }
            }

            is TimerStatus.Snoozed -> {
                val remainingMs = current.deadlineTimestampMs - now
                if (remainingMs <= 0L) {
                    triggerBreak()
                } else {
                    val remainingSeconds = ceilSeconds(remainingMs)
                    if (remainingSeconds != current.remainingSeconds) {
                        _state.value = current.copy(remainingSeconds = remainingSeconds)
                    }
                }
            }

            is TimerStatus.Idle -> Unit
        }
    }

    private fun workTotalSeconds(): Int = prefs.workDurationMinutes * 60

    private fun freshWorking(pauseReason: PauseReason): TimerStatus.Working {
        val totalSec = workTotalSeconds()
        return if (pauseReason == PauseReason.NONE) {
            TimerStatus.Working(
                remainingSeconds = totalSec,
                elapsedSeconds = 0,
                totalSeconds = totalSec,
                pauseReason = PauseReason.NONE,
                deadlineTimestampMs = timeProvider() + totalSec * 1000L
            )
        } else {
            TimerStatus.Working(
                remainingSeconds = totalSec,
                elapsedSeconds = 0,
                totalSeconds = totalSec,
                pauseReason = pauseReason,
                remainingAtPauseMs = totalSec * 1000L
            )
        }
    }

    private fun ceilSeconds(ms: Long): Int = ((ms + 999L) / 1000L).toInt()
}
