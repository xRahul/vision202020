package com.vision202020.app.domain

import com.vision202020.app.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VisionTimerEngine(
    private var prefs: UserPreferences,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val _state = MutableStateFlow<TimerStatus>(TimerStatus.Idle)
    val state: StateFlow<TimerStatus> = _state.asStateFlow()

    private var _isSessionActive: Boolean = false
    val isSessionActive: Boolean get() = _isSessionActive

    var onBreakCompleted: (() -> Unit)? = null

    private var screenOffTimestampMs: Long = 0L

    fun start() {
        _isSessionActive = true
        val totalSec = prefs.workDurationMinutes * 60
        _state.value = TimerStatus.Working(
            remainingSeconds = totalSec,
            elapsedSeconds = 0,
            totalSeconds = totalSec,
            isScreenPaused = false
        )
    }

    fun pause() {
        val current = _state.value
        if (current is TimerStatus.Working && !current.isScreenPaused) {
            _state.value = current.copy(isScreenPaused = true)
        }
    }

    fun resume() {
        val current = _state.value
        if (current is TimerStatus.Working && current.isScreenPaused) {
            _state.value = current.copy(isScreenPaused = false)
        }
    }

    fun stop() {
        _isSessionActive = false
        screenOffTimestampMs = 0L
        _state.value = TimerStatus.Idle
    }

    fun updatePreferences(newPrefs: UserPreferences) {
        prefs = newPrefs

        val current = _state.value
        if (current is TimerStatus.Working) {
            val newTotalSec = newPrefs.workDurationMinutes * 60
            if (current.elapsedSeconds >= newTotalSec) {
                // Elapsed time exceeds new duration -> trigger immediate break
                triggerBreak()
            } else {
                val newRemaining = (newTotalSec - current.elapsedSeconds).coerceAtLeast(0)
                _state.value = current.copy(
                    remainingSeconds = newRemaining,
                    totalSeconds = newTotalSec
                )
            }
        }
    }

    fun onScreenOff() {
        screenOffTimestampMs = timeProvider()
        val current = _state.value
        if (current is TimerStatus.Working) {
            _state.value = current.copy(isScreenPaused = true)
        }
    }

    fun onScreenOn() {
        val now = timeProvider()
        val offDurationMs = if (screenOffTimestampMs > 0L) now - screenOffTimestampMs else 0L
        val naturalBreakThresholdMs = prefs.naturalBreakMinutes * 60 * 1000L

        val current = _state.value
        if (_isSessionActive) {
            if (screenOffTimestampMs > 0L && offDurationMs >= naturalBreakThresholdMs) {
                // Natural break (>= naturalBreakMinutes screen off): reset accumulator and auto-start fresh countdown
                val totalSec = prefs.workDurationMinutes * 60
                _state.value = TimerStatus.Working(
                    remainingSeconds = totalSec,
                    elapsedSeconds = 0,
                    totalSeconds = totalSec,
                    isScreenPaused = false
                )
            } else if (current is TimerStatus.Working) {
                _state.value = current.copy(isScreenPaused = false)
            } else if (current is TimerStatus.Idle) {
                val totalSec = prefs.workDurationMinutes * 60
                _state.value = TimerStatus.Working(
                    remainingSeconds = totalSec,
                    elapsedSeconds = 0,
                    totalSeconds = totalSec,
                    isScreenPaused = false
                )
            }
        }
        screenOffTimestampMs = 0L
    }

    fun snooze(durationSeconds: Int = 300) {
        _state.value = TimerStatus.Snoozed(
            remainingSeconds = durationSeconds,
            totalSeconds = durationSeconds
        )
    }

    fun completeBreak() {
        val totalSec = prefs.workDurationMinutes * 60
        val isScreenCurrentlyOff = screenOffTimestampMs > 0L
        _state.value = TimerStatus.Working(
            remainingSeconds = totalSec,
            elapsedSeconds = 0,
            totalSeconds = totalSec,
            isScreenPaused = isScreenCurrentlyOff
        )
        onBreakCompleted?.invoke()
    }

    fun triggerBreak() {
        _state.value = TimerStatus.RestBreak(
            remainingSeconds = prefs.restDurationSeconds,
            totalSeconds = prefs.restDurationSeconds,
            breakStartedTimestampMs = timeProvider()
        )
    }

    fun tickSecond() {
        when (val current = _state.value) {
            is TimerStatus.Working -> {
                if (!current.isScreenPaused) {
                    val newElapsed = current.elapsedSeconds + 1
                    val newRemaining = current.remainingSeconds - 1
                    if (newRemaining <= 0) {
                        triggerBreak()
                    } else {
                        _state.value = current.copy(
                            remainingSeconds = newRemaining,
                            elapsedSeconds = newElapsed
                        )
                    }
                }
            }
            is TimerStatus.RestBreak -> {
                val now = timeProvider()
                val elapsedSinceBreakStarted = ((now - current.breakStartedTimestampMs) / 1000L).toInt()
                val newRemaining = (current.totalSeconds - elapsedSinceBreakStarted).coerceAtLeast(0)
                if (newRemaining <= 0) {
                    completeBreak()
                } else {
                    _state.value = current.copy(remainingSeconds = newRemaining)
                }
            }
            is TimerStatus.Snoozed -> {
                val newRemaining = current.remainingSeconds - 1
                if (newRemaining <= 0) {
                    triggerBreak()
                } else {
                    _state.value = current.copy(remainingSeconds = newRemaining)
                }
            }
            is TimerStatus.Idle -> {
                // No-op
            }
        }
    }
}
