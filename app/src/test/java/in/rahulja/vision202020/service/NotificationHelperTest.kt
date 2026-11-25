package `in`.rahulja.vision202020.service

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.data.UserPreferences
import `in`.rahulja.vision202020.domain.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationHelperTest {

    private lateinit var context: Context
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationHelper = NotificationHelper(context)
    }

    @Test
    fun testNotificationChannelsCreated() {
        notificationHelper.createChannels()

        val timerChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID_TIMER)
        val breakChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID_BREAK)

        assertNotNull("Timer channel must exist", timerChannel)
        assertNotNull("Break channel must exist", breakChannel)
        assertEquals(NotificationManager.IMPORTANCE_LOW, timerChannel.importance)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, breakChannel.importance)
    }

    @Test
    fun testBuildOngoingNotificationWorkingState() {
        notificationHelper.createChannels()
        val status = TimerStatus.Working(
            remainingSeconds = 1195,
            elapsedSeconds = 5,
            totalSeconds = 1200
        )

        val notification = notificationHelper.buildTimerNotification(status)
        assertNotNull(notification)
        assertEquals(NotificationHelper.CHANNEL_ID_TIMER, notification.channelId)
        val actions = notification.actions
        assertNotNull(actions)
        // Working state should have Pause, Take Break, Reset, Stop
        assertEquals(4, actions.size)
        assertEquals("Pause", actions[0].title.toString())
        assertEquals("Rest Now", actions[1].title.toString())
        assertEquals("Reset", actions[2].title.toString())
        assertEquals("Stop", actions[3].title.toString())
    }

    @Test
    fun testBuildNotificationIdleStateHasStartAction() {
        notificationHelper.createChannels()
        val notification = notificationHelper.buildTimerNotification(TimerStatus.Idle)
        assertNotNull(notification)
        val actions = notification.actions
        assertNotNull(actions)
        assertEquals(1, actions.size)
        assertEquals("Start", actions[0].title.toString())
    }

    private val breakStatus = TimerStatus.RestBreak(remainingSeconds = 15, totalSeconds = 20, breakStartedTimestampMs = 1000L)

    @Test
    fun strictBreakAlertHasOnlySnoozeAction() {
        notificationHelper.createChannels()
        val notification = notificationHelper.buildBreakAlertNotification(
            breakStatus,
            UserPreferences(strictMode = true)
        )
        val titles = notification.actions.map { it.title.toString() }
        assertEquals(listOf("Snooze 5m"), titles)
    }

    @Test
    fun nonStrictBreakAlertHasLookedAwayAndSnooze() {
        notificationHelper.createChannels()
        val notification = notificationHelper.buildBreakAlertNotification(
            breakStatus,
            UserPreferences(strictMode = false)
        )
        val titles = notification.actions.map { it.title.toString() }
        assertEquals(listOf("I Looked Away", "Snooze 5m"), titles)
    }

    @Test
    fun strictTimerNotificationDuringBreakHasNoLookedAway() {
        notificationHelper.createChannels()
        val notification = notificationHelper.buildTimerNotification(breakStatus, 5, strictMode = true)
        val titles = notification.actions.map { it.title.toString() }
        assertEquals(listOf("Snooze 5m", "Stop"), titles)
    }

    @Test
    fun breakAlertCountsDownWithChronometerInsteadOfProgress() {
        notificationHelper.createChannels()
        val before = System.currentTimeMillis()
        val notification = notificationHelper.buildBreakAlertNotification(breakStatus, UserPreferences())

        assertTrue(notification.extras.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(notification.extras.getBoolean(android.app.Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertTrue(notification.`when` >= before + 15_000L)
        assertEquals(0, notification.extras.getInt(android.app.Notification.EXTRA_PROGRESS_MAX))
    }

    @Test
    fun testFormatTime() {
        assertEquals("20:00", NotificationHelper.formatTime(1200))
        assertEquals("00:20", NotificationHelper.formatTime(20))
        assertEquals("05:09", NotificationHelper.formatTime(309))
    }
}
