package `in`.rahulja.vision202020.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSnapshotCodecTest {

    @Test
    fun `working snapshot round-trips`() {
        val snapshot = SessionSnapshot(
            status = TimerStatus.Working(
                remainingSeconds = 500,
                elapsedSeconds = 700,
                totalSeconds = 1200,
                pauseReason = PauseReason.MANUAL,
                remainingAtPauseMs = 500_000L
            ),
            isSessionActive = true
        )

        val decoded = SessionSnapshotCodec.decode(SessionSnapshotCodec.encode(snapshot))

        assertEquals(snapshot, decoded)
    }

    @Test
    fun `rest break and snoozed snapshots round-trip`() {
        val rest = SessionSnapshot(
            TimerStatus.RestBreak(remainingSeconds = 12, totalSeconds = 20, breakStartedTimestampMs = 999L),
            true
        )
        val snoozed = SessionSnapshot(
            TimerStatus.Snoozed(remainingSeconds = 200, totalSeconds = 300, deadlineTimestampMs = 1234L),
            true
        )

        assertEquals(rest, SessionSnapshotCodec.decode(SessionSnapshotCodec.encode(rest)))
        assertEquals(snoozed, SessionSnapshotCodec.decode(SessionSnapshotCodec.encode(snoozed)))
    }

    @Test
    fun `idle snapshot round-trips`() {
        val idle = SessionSnapshot(TimerStatus.Idle, false)
        assertEquals(idle, SessionSnapshotCodec.decode(SessionSnapshotCodec.encode(idle)))
    }

    @Test
    fun `corrupt or foreign payloads decode to null`() {
        assertNull(SessionSnapshotCodec.decode(null))
        assertNull(SessionSnapshotCodec.decode(""))
        assertNull(SessionSnapshotCodec.decode("garbage"))
        assertNull(SessionSnapshotCodec.decode("1|1|X"))
        assertNull(SessionSnapshotCodec.decode("1|1|W:notanumber:0:1200:NONE:0:0"))
        assertNull(SessionSnapshotCodec.decode("2|1|I"))
    }

    @Test
    fun `session key ignores per-second tick updates`() {
        val running = TimerStatus.Working(
            remainingSeconds = 100,
            elapsedSeconds = 20,
            totalSeconds = 120,
            deadlineTimestampMs = 5_000L
        )
        val ticked = running.copy(remainingSeconds = 99, elapsedSeconds = 21)

        assertEquals(running.sessionKey(), ticked.sessionKey())
        assertTrue(running.sessionKey() != TimerStatus.Idle.sessionKey())
    }
}
