package `in`.rahulja.vision202020.ui.text

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.domain.PauseReason
import `in`.rahulja.vision202020.domain.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TimerStatusTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun working(reason: PauseReason = PauseReason.NONE) = TimerStatus.Working(
        remainingSeconds = 1195,
        elapsedSeconds = 5,
        totalSeconds = 1200,
        pauseReason = reason
    )

    private val restBreak = TimerStatus.RestBreak(remainingSeconds = 18, totalSeconds = 20, breakStartedTimestampMs = 1000L)
    private val snoozed = TimerStatus.Snoozed(remainingSeconds = 240, totalSeconds = 300)

    @Test
    fun titleIdle() {
        assertEquals("20-20-20 Ready", TimerStatusText.title(context, TimerStatus.Idle))
    }

    @Test
    fun titleWorking() {
        assertEquals("19:55 remaining", TimerStatusText.title(context, working()))
    }

    @Test
    fun titlePaused() {
        assertEquals("Paused: 19:55", TimerStatusText.title(context, working(PauseReason.MANUAL)))
    }

    @Test
    fun titleBreak() {
        assertEquals("Rest eyes: 18s", TimerStatusText.title(context, restBreak))
    }

    @Test
    fun titleSnoozed() {
        assertEquals("Snoozed: 04:00", TimerStatusText.title(context, snoozed))
    }

    @Test
    fun actionLabelIdle() {
        assertEquals("Start", TimerStatusText.actionLabel(context, TimerStatus.Idle, strictMode = false))
    }

    @Test
    fun actionLabelWorking() {
        assertEquals("Pause", TimerStatusText.actionLabel(context, working(), strictMode = false))
    }

    @Test
    fun actionLabelPaused() {
        assertEquals("Resume", TimerStatusText.actionLabel(context, working(PauseReason.SCREEN_OFF), strictMode = false))
    }

    @Test
    fun actionLabelBreak() {
        assertEquals("Skip", TimerStatusText.actionLabel(context, restBreak, strictMode = false))
    }

    @Test
    fun actionLabelStrictBreakDoesNotPromiseSkip() {
        assertEquals("Open", TimerStatusText.actionLabel(context, restBreak, strictMode = true))
    }

    @Test
    fun actionLabelSnoozed() {
        assertEquals("Rest Now", TimerStatusText.actionLabel(context, snoozed, strictMode = true))
    }
}
