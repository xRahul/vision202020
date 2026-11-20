package `in`.rahulja.vision202020.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VisionTimerStateHolderTest {

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        assertEquals(TimerStatus.Idle, VisionTimerStateHolder.state.value)
    }

    @Test
    fun `state transitions update shared flow`() = runTest {
        val job = launch { VisionTimerStateHolder.state.collect { it } }
        VisionTimerStateHolder.setState(
            TimerStatus.Working(remainingSeconds = 1200, elapsedSeconds = 0, totalSeconds = 1200)
        )
        assertEquals(
            TimerStatus.Working(remainingSeconds = 1200, elapsedSeconds = 0, totalSeconds = 1200),
            VisionTimerStateHolder.state.value
        )
        job.cancel()
    }

    @Test
    fun `state transitions to RestBreak`() = runTest {
        VisionTimerStateHolder.setState(TimerStatus.RestBreak(remainingSeconds = 20, totalSeconds = 20, breakStartedTimestampMs = 0L))
        assertEquals(TimerStatus.RestBreak(remainingSeconds = 20, totalSeconds = 20, breakStartedTimestampMs = 0L), VisionTimerStateHolder.state.value)
    }

    @Test
    fun `state transitions to Snoozed`() = runTest {
        VisionTimerStateHolder.setState(TimerStatus.Snoozed(remainingSeconds = 60, totalSeconds = 60))
        assertEquals(TimerStatus.Snoozed(remainingSeconds = 60, totalSeconds = 60), VisionTimerStateHolder.state.value)
    }

    @Test
    fun `state transitions back to Idle`() = runTest {
        VisionTimerStateHolder.setState(TimerStatus.Idle)
        assertEquals(TimerStatus.Idle, VisionTimerStateHolder.state.value)
    }

    @Test
    fun `state retains latest value`() = runTest {
        val expected = TimerStatus.Working(remainingSeconds = 600, elapsedSeconds = 600, totalSeconds = 1200)
        VisionTimerStateHolder.setState(expected)
        assertTrue(VisionTimerStateHolder.state.value is TimerStatus.Working)
        assertEquals(expected, VisionTimerStateHolder.state.value)
    }
}