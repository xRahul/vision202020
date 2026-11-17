package com.vision202020.app.service

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vision202020.app.domain.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
            totalSeconds = 1200,
            isScreenPaused = false
        )

        val notification = notificationHelper.buildTimerNotification(status)
        assertNotNull(notification)
        assertEquals(NotificationHelper.CHANNEL_ID_TIMER, notification.channelId)
    }

    @Test
    fun testFormatTime() {
        assertEquals("20:00", NotificationHelper.formatTime(1200))
        assertEquals("00:20", NotificationHelper.formatTime(20))
        assertEquals("05:09", NotificationHelper.formatTime(309))
    }
}
