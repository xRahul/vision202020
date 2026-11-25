package `in`.rahulja.vision202020.domain

enum class PauseReason {
    NONE,
    MANUAL,
    SCREEN_OFF
}

sealed interface TimerStatus {
    data object Idle : TimerStatus

    data class Working(
        val remainingSeconds: Int,
        val elapsedSeconds: Int,
        val totalSeconds: Int,
        val pauseReason: PauseReason = PauseReason.NONE,
        // Absolute SystemClock.elapsedRealtime() deadline used while running (monotonic, unaffected by wall-clock changes).
        val deadlineTimestampMs: Long = 0L,
        // Milliseconds left when the timer was paused, used to resume exactly.
        val remainingAtPauseMs: Long = 0L
    ) : TimerStatus {
        val isScreenPaused: Boolean get() = pauseReason != PauseReason.NONE
    }

    data class RestBreak(val remainingSeconds: Int, val totalSeconds: Int, val breakStartedTimestampMs: Long) : TimerStatus

    data class Snoozed(val remainingSeconds: Int, val totalSeconds: Int, val deadlineTimestampMs: Long = 0L) : TimerStatus
}

/**
 * Identifies a state transition worth persisting. Per-second tick updates keep the
 * same key, so callers can persist session state without writing once per second.
 */
fun TimerStatus.sessionKey(): String = when (this) {
    is TimerStatus.Idle -> "I"

    is TimerStatus.Working ->
        "W:$totalSeconds:$pauseReason:$deadlineTimestampMs:$remainingAtPauseMs"

    is TimerStatus.RestBreak -> "R:$totalSeconds:$breakStartedTimestampMs"

    is TimerStatus.Snoozed -> "S:$totalSeconds:$deadlineTimestampMs"
}
