package `in`.rahulja.vision202020.tile

import android.content.Context
import android.service.quicksettings.Tile
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.domain.PauseReason
import `in`.rahulja.vision202020.domain.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class VisionTileServiceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testTileStateMappingForIdle() {
        val (state, subtitle) = VisionTileService.mapStatusToTileState(context, TimerStatus.Idle)
        assertEquals(Tile.STATE_INACTIVE, state)
        assertEquals("Tap to start", subtitle)
    }

    @Test
    fun testTileStateMappingForWorking() {
        val status = TimerStatus.Working(
            remainingSeconds = 1195,
            elapsedSeconds = 5,
            totalSeconds = 1200,
            pauseReason = PauseReason.NONE
        )
        val (state, subtitle) = VisionTileService.mapStatusToTileState(context, status)
        assertEquals(Tile.STATE_ACTIVE, state)
        assertEquals("19:55 left", subtitle)
    }

    @Test
    fun testTileStateMappingForPaused() {
        val status = TimerStatus.Working(
            remainingSeconds = 600,
            elapsedSeconds = 600,
            totalSeconds = 1200,
            pauseReason = PauseReason.MANUAL
        )
        val (state, subtitle) = VisionTileService.mapStatusToTileState(context, status)
        assertEquals(Tile.STATE_INACTIVE, state)
        assertEquals("Paused (10:00)", subtitle)
    }

    @Test
    fun testTileStateMappingForSnoozed() {
        val status = TimerStatus.Snoozed(
            remainingSeconds = 240,
            totalSeconds = 300,
            deadlineTimestampMs = 1000L
        )
        val (state, subtitle) = VisionTileService.mapStatusToTileState(context, status)
        assertEquals(Tile.STATE_INACTIVE, state)
        assertEquals("Snoozed (04:00)", subtitle)
    }

    @Test
    fun testTileStateMappingForBreak() {
        val status = TimerStatus.RestBreak(
            remainingSeconds = 18,
            totalSeconds = 20,
            breakStartedTimestampMs = 1000L
        )
        val (state, subtitle) = VisionTileService.mapStatusToTileState(context, status)
        assertEquals(Tile.STATE_ACTIVE, state)
        assertEquals("Rest: 18s", subtitle)
    }
}
