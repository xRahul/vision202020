package `in`.rahulja.vision202020.tile

import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.domain.PauseReason
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.domain.VisionTimerStateHolder
import `in`.rahulja.vision202020.ui.breakui.BreakActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

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

    private fun strictBreakTile(locked: Boolean): Triple<VisionTileService, MutableList<Intent>, MutableList<Boolean>> {
        VisionTimerStateHolder.setState(
            TimerStatus.RestBreak(remainingSeconds = 15, totalSeconds = 20, breakStartedTimestampMs = 1000L)
        )
        val tile = Robolectric.setupService(VisionTileService::class.java)
        val launched = mutableListOf<Intent>()
        val unlocked = mutableListOf<Boolean>()
        tile.strictMode = true
        tile.isDeviceLocked = { locked }
        tile.runUnlocked = { action ->
            unlocked.add(true)
            action.run()
        }
        tile.launchActivity = { launched.add(it) }
        return Triple(tile, launched, unlocked)
    }

    @Test
    fun strictBreakClickOpensBreakActivity() {
        val (tile, launched, unlocked) = strictBreakTile(locked = false)

        tile.onClick()

        assertEquals(1, launched.size)
        assertEquals(BreakActivity::class.java.name, launched[0].component?.className)
        assertTrue("unlock must not be requested when unlocked", unlocked.isEmpty())
        VisionTimerStateHolder.setState(TimerStatus.Idle)
    }

    @Test
    fun strictBreakClickWhenLockedUnlocksFirst() {
        val (tile, launched, unlocked) = strictBreakTile(locked = true)

        tile.onClick()

        assertEquals(1, unlocked.size)
        assertNotNull(launched.singleOrNull())
        VisionTimerStateHolder.setState(TimerStatus.Idle)
    }

    @Test
    fun nonStrictBreakClickDoesNotOpenBreakActivity() {
        val (tile, launched, _) = strictBreakTile(locked = false)
        tile.strictMode = false

        tile.onClick()

        assertFalse(launched.any { it.component?.className == BreakActivity::class.java.name })
        VisionTimerStateHolder.setState(TimerStatus.Idle)
    }
}
