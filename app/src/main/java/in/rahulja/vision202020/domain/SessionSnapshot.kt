package `in`.rahulja.vision202020.domain

/**
 * Survives process death. Absolute timestamps make restore accurate: the engine
 * recomputes remaining time from the deadline instead of trusting a tick count.
 */
data class SessionSnapshot(
    val status: TimerStatus,
    val isSessionActive: Boolean,
    /** `Settings.Global.BOOT_COUNT` at save time, -1 if unavailable. */
    val bootCount: Int,
    /** elapsedRealtime at screen-off; 0 means the screen is on. */
    val screenOffAtMs: Long
) {
    /** A restored session is only meaningful if it was active and not already Idle. */
    val isResumable: Boolean get() = isSessionActive && status !is TimerStatus.Idle
}

object SessionSnapshotCodec {

    const val VERSION = "2"

    fun encode(snapshot: SessionSnapshot): String {
        val active = if (snapshot.isSessionActive) "1" else "0"
        val body = when (val status = snapshot.status) {
            is TimerStatus.Idle -> "I"

            is TimerStatus.Working -> listOf(
                "W",
                status.remainingSeconds,
                status.elapsedSeconds,
                status.totalSeconds,
                status.pauseReason.name,
                status.deadlineTimestampMs,
                status.remainingAtPauseMs
            ).joinToString(":")

            is TimerStatus.RestBreak -> listOf(
                "R",
                status.remainingSeconds,
                status.totalSeconds,
                status.breakStartedTimestampMs
            ).joinToString(":")

            is TimerStatus.Snoozed -> listOf(
                "S",
                status.remainingSeconds,
                status.totalSeconds,
                status.deadlineTimestampMs
            ).joinToString(":")
        }
        return "$VERSION|$active|${snapshot.bootCount}|${snapshot.screenOffAtMs}|$body"
    }

    fun decode(raw: String?): SessionSnapshot? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split("|")
        if (parts.size != 5 || parts[0] != VERSION) return null
        val isActive = when (parts[1]) {
            "1" -> true
            "0" -> false
            else -> return null
        }
        val bootCount = parts[2].toIntOrNull() ?: return null
        val screenOffAtMs = parts[3].toLongOrNull() ?: return null
        val fields = parts[4].split(":")
        val status = try {
            when (fields[0]) {
                "I" -> TimerStatus.Idle

                "W" -> TimerStatus.Working(
                    remainingSeconds = fields[1].toInt(),
                    elapsedSeconds = fields[2].toInt(),
                    totalSeconds = fields[3].toInt(),
                    pauseReason = PauseReason.valueOf(fields[4]),
                    deadlineTimestampMs = fields[5].toLong(),
                    remainingAtPauseMs = fields[6].toLong()
                )

                "R" -> TimerStatus.RestBreak(
                    remainingSeconds = fields[1].toInt(),
                    totalSeconds = fields[2].toInt(),
                    breakStartedTimestampMs = fields[3].toLong()
                )

                "S" -> TimerStatus.Snoozed(
                    remainingSeconds = fields[1].toInt(),
                    totalSeconds = fields[2].toInt(),
                    deadlineTimestampMs = fields[3].toLong()
                )

                else -> return null
            }
        } catch (e: RuntimeException) {
            // Corrupt or truncated snapshot: start clean rather than crash on boot.
            return null
        }
        return SessionSnapshot(status, isActive, bootCount, screenOffAtMs)
    }
}
