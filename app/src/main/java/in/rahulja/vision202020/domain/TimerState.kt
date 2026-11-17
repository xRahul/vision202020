package `in`.rahulja.vision202020.domain

sealed interface TimerStatus {
    data object Idle : TimerStatus

    data class Working(
        val remainingSeconds: Int,
        val elapsedSeconds: Int,
        val totalSeconds: Int,
        val isScreenPaused: Boolean
    ) : TimerStatus

    data class RestBreak(
        val remainingSeconds: Int,
        val totalSeconds: Int,
        val breakStartedTimestampMs: Long
    ) : TimerStatus

    data class Snoozed(
        val remainingSeconds: Int,
        val totalSeconds: Int
    ) : TimerStatus
}
