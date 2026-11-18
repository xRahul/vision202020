package `in`.rahulja.vision202020.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import `in`.rahulja.vision202020.R
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.ui.breakui.BreakActivity
import `in`.rahulja.vision202020.ui.main.MainActivity
import java.util.Locale

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for ongoing timer ticker (low importance, no intrusive sound every second)
            val timerChannel = NotificationChannel(
                CHANNEL_ID_TIMER,
                context.getString(R.string.channel_timer_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_timer_desc)
                setShowBadge(false)
            }

            // Channel for break alert (high importance, sound and vibration)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val breakChannel = NotificationChannel(
                CHANNEL_ID_BREAK,
                context.getString(R.string.channel_break_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_break_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setSound(defaultSoundUri, audioAttributes)
            }

            notificationManager.createNotificationChannel(timerChannel)
            notificationManager.createNotificationChannel(breakChannel)
        }
    }

    fun buildTimerNotification(status: TimerStatus): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val (title, contentText, isOngoing) = when (status) {
            is TimerStatus.Working -> {
                val timeStr = formatTime(status.remainingSeconds)
                if (status.isScreenPaused) {
                    Triple(
                        context.getString(R.string.notif_title_paused),
                        context.getString(R.string.notif_content_paused, timeStr),
                        true
                    )
                } else {
                    Triple(
                        context.getString(R.string.notif_title_working),
                        context.getString(R.string.notif_content_working, timeStr),
                        true
                    )
                }
            }
            is TimerStatus.RestBreak -> {
                val timeStr = formatTime(status.remainingSeconds)
                Triple(
                    context.getString(R.string.notif_title_break),
                    context.getString(R.string.notif_content_break, timeStr),
                    true
                )
            }
            is TimerStatus.Snoozed -> {
                val timeStr = formatTime(status.remainingSeconds)
                Triple(
                    context.getString(R.string.notif_title_snoozed),
                    context.getString(R.string.notif_content_snoozed, timeStr),
                    true
                )
            }
            is TimerStatus.Idle -> {
                Triple(
                    context.getString(R.string.notif_title_idle),
                    context.getString(R.string.notif_content_idle),
                    false
                )
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_TIMER)
            .setSmallIcon(R.drawable.ic_stat_vision)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isOngoing)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        // Action buttons tailored to current state
        when (status) {
            is TimerStatus.Working -> {
                if (status.isScreenPaused) {
                    val resumeIntent = Intent(context, VisionTimerService::class.java).apply {
                        action = VisionTimerService.ACTION_RESUME
                    }
                    val resumePending = PendingIntent.getService(
                        context, 1, resumeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    builder.addAction(android.R.drawable.ic_media_play, context.getString(R.string.action_resume), resumePending)
                } else {
                    val pauseIntent = Intent(context, VisionTimerService::class.java).apply {
                        action = VisionTimerService.ACTION_PAUSE
                    }
                    val pausePending = PendingIntent.getService(
                        context, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    builder.addAction(android.R.drawable.ic_media_pause, context.getString(R.string.action_pause), pausePending)
                }

                val skipIntent = Intent(context, VisionTimerService::class.java).apply {
                    action = VisionTimerService.ACTION_TRIGGER_BREAK
                }
                val skipPending = PendingIntent.getService(
                    context, 3, skipIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(android.R.drawable.ic_media_next, context.getString(R.string.action_take_break), skipPending)

                val stopIntent = Intent(context, VisionTimerService::class.java).apply {
                    action = VisionTimerService.ACTION_STOP
                }
                val stopPending = PendingIntent.getService(
                    context, 4, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.action_stop), stopPending)
            }
            is TimerStatus.RestBreak, is TimerStatus.Snoozed -> {
                val stopIntent = Intent(context, VisionTimerService::class.java).apply {
                    action = VisionTimerService.ACTION_STOP
                }
                val stopPending = PendingIntent.getService(
                    context, 4, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.action_stop), stopPending)
            }
            is TimerStatus.Idle -> {
                val startIntent = Intent(context, VisionTimerService::class.java).apply {
                    action = VisionTimerService.ACTION_START
                }
                val startPending = PendingIntent.getService(
                    context, 0, startIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(android.R.drawable.ic_media_play, context.getString(R.string.action_start), startPending)
            }
        }

        return builder.build()
    }

    fun buildBreakAlertNotification(remainingSeconds: Int): Notification {
        val fullScreenIntent = Intent(context, BreakActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            5,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val snoozeIntent = Intent(context, VisionTimerService::class.java).apply {
            action = VisionTimerService.ACTION_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getService(
            context, 6, snoozeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val completeIntent = Intent(context, VisionTimerService::class.java).apply {
            action = VisionTimerService.ACTION_COMPLETE_BREAK
        }
        val completePendingIntent = PendingIntent.getService(
            context, 7, completeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_BREAK)
            .setSmallIcon(R.drawable.ic_stat_vision)
            .setContentTitle(context.getString(R.string.break_alert_title))
            .setContentText(context.getString(R.string.break_alert_content))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_view, context.getString(R.string.action_looked_away), completePendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, context.getString(R.string.action_snooze), snoozePendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID_TIMER = "channel_vision_timer"
        const val CHANNEL_ID_BREAK = "channel_vision_break"
        const val NOTIFICATION_ID_TIMER = 101
        const val NOTIFICATION_ID_BREAK = 102

        fun formatTime(totalSeconds: Int): String {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }
}
