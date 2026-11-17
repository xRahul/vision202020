package com.vision202020.app.tile

import android.service.quicksettings.Tile
import com.vision202020.app.domain.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class VisionTileServiceTest {

    @Test
    fun testTileStateMappingForIdle() {
        val (state, subtitle) = VisionTileService.mapStatusToTileState(TimerStatus.Idle)
        assertEquals(Tile.STATE_INACTIVE, state)
        assertEquals("Tap to start", subtitle)
    }

    @Test
    fun testTileStateMappingForWorking() {
        val status = TimerStatus.Working(
            remainingSeconds = 1195,
            elapsedSeconds = 5,
            totalSeconds = 1200,
            isScreenPaused = false
        )
        val (state, subtitle) = VisionTileService.mapStatusToTileState(status)
        assertEquals(Tile.STATE_ACTIVE, state)
        assertEquals("19:55 left", subtitle)
    }

    @Test
    fun testTileStateMappingForPaused() {
        val status = TimerStatus.Working(
            remainingSeconds = 600,
            elapsedSeconds = 600,
            totalSeconds = 1200,
            isScreenPaused = true
        )
        val (state, subtitle) = VisionTileService.mapStatusToTileState(status)
        assertEquals(Tile.STATE_INACTIVE, state)
        assertEquals("Paused (10:00)", subtitle)
    }

    @Test
    fun testTileStateMappingForBreak() {
        val status = TimerStatus.RestBreak(
            remainingSeconds = 18,
            totalSeconds = 20,
            breakStartedTimestampMs = 1000L
        )
        val (state, subtitle) = VisionTileService.mapStatusToTileState(status)
        assertEquals(Tile.STATE_ACTIVE, state)
        assertEquals("Rest: 18s", subtitle)
    }
}
