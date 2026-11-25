package `in`.rahulja.vision202020.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import `in`.rahulja.vision202020.R
import `in`.rahulja.vision202020.data.UserPreferences
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.ui.breakui.BreakLauncher
import `in`.rahulja.vision202020.ui.main.MainActivity
import java.util.Locale

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels() {
        // Channel for ongoing timer ticker (low importance, no intrusive sound every second)
        val timerChannel = NotificationChannel(
            CHANNEL_ID_TIMER,
            context.getString(R.string.channel_timer_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_timer_desc)
            setShowBadge(false)
        }

        // Break channel intentionally carries no sound/vibration. SoundAlertHelper owns
        // both so the "Sound" and "Vibration" preferences are the single source of truth.
        // Versioned id so existing installs pick up the silent configuration.
        val breakChannel = NotificationChannel(
            CHANNEL_ID_BREAK,
            context.getString(R.string.channel_break_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_break_desc)
            setSound(null, null)
            enableVibration(false)
        }

        notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID_BREAK)
        notificationManager.createNotificationChannel(timerChannel)
        notificationManager.createNotificationChannel(breakChannel)
    }

    fun buildTimerNotification(status: TimerStatus, snoozeMinutes: Int = 5, strictMode: Boolean = false): Notification {
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

            is TimerStatus.RestBreak -> Triple(
                context.getString(R.string.notif_title_break),
                context.getString(R.string.notif_content_break, formatTime(status.remainingSeconds)),
                true
            )

            is TimerStatus.Snoozed -> Triple(
                context.getString(R.string.notif_title_snoozed),
                context.getString(R.string.notif_content_snoozed, formatTime(status.remainingSeconds)),
                true
            )

            is TimerStatus.Idle -> Triple(
                context.getString(R.string.notif_title_idle),
                context.getString(R.string.notif_content_idle),
                false
            )
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

        when (status) {
            is TimerStatus.Working -> {
                builder.setProgress(status.totalSeconds, status.remainingSeconds, false)
                if (status.isScreenPaused) {
                    builder.addAction(
                        android.R.drawable.ic_media_play,
                        context.getString(R.string.action_resume),
                        servicePendingIntent(1, VisionTimerService.ACTION_RESUME)
                    )
                } else {
                    builder.addAction(
                        android.R.drawable.ic_media_pause,
                        context.getString(R.string.action_pause),
                        servicePendingIntent(2, VisionTimerService.ACTION_PAUSE)
                    )
                }
                builder.addAction(
                    android.R.drawable.ic_media_next,
                    context.getString(R.string.action_take_break),
                    servicePendingIntent(3, VisionTimerService.ACTION_TRIGGER_BREAK)
                )
                builder.addAction(
                    android.R.drawable.ic_menu_revert,
                    context.getString(R.string.action_reset),
                    servicePendingIntent(8, VisionTimerService.ACTION_RESET)
                )
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.action_stop),
                    servicePendingIntent(4, VisionTimerService.ACTION_STOP)
                )
            }

            is TimerStatus.RestBreak -> {
                builder.setProgress(status.totalSeconds, status.remainingSeconds, false)
                // Strict mode: skipping needs the hold on the break screen, not a one-tap action.
                if (!strictMode) {
                    builder.addAction(
                        android.R.drawable.ic_menu_view,
                        context.getString(R.string.action_looked_away),
                        servicePendingIntent(7, VisionTimerService.ACTION_COMPLETE_BREAK)
                    )
                }
                builder.addAction(
                    android.R.drawable.ic_lock_idle_alarm,
                    context.getString(R.string.action_snooze, snoozeMinutes),
                    servicePendingIntent(6, VisionTimerService.ACTION_SNOOZE)
                )
                // Stop would end the break in one tap, so strict mode leaves it to the main app.
                if (!strictMode) {
                    builder.addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        context.getString(R.string.action_stop),
                        servicePendingIntent(4, VisionTimerService.ACTION_STOP)
                    )
                }
            }

            is TimerStatus.Snoozed -> {
                builder.setProgress(status.totalSeconds, status.remainingSeconds, false)
                builder.addAction(
                    android.R.drawable.ic_media_next,
                    context.getString(R.string.action_take_break),
                    servicePendingIntent(3, VisionTimerService.ACTION_TRIGGER_BREAK)
                )
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.action_stop),
                    servicePendingIntent(4, VisionTimerService.ACTION_STOP)
                )
            }

            is TimerStatus.Idle -> {
                builder.addAction(
                    android.R.drawable.ic_media_play,
                    context.getString(R.string.action_start),
                    servicePendingIntent(0, VisionTimerService.ACTION_START)
                )
            }
        }

        return builder.build()
    }

    fun buildBreakAlertNotification(status: TimerStatus.RestBreak, preferences: UserPreferences): Notification {
        val fullScreenPendingIntent = BreakLauncher.pendingIntent(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_BREAK)
            .setSmallIcon(R.drawable.ic_stat_vision)
            .setContentTitle(context.getString(R.string.break_alert_title))
            .setContentText(
                context.getString(
                    R.string.notif_content_break,
                    formatTime(status.remainingSeconds)
                )
            )
            .setSubText(context.getString(R.string.break_alert_content))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSilent(!preferences.soundEnabled)
            .setOnlyAlertOnce(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            // Posted once per break; the system counts down, so no per-second reposts.
            .setWhen(System.currentTimeMillis() + status.remainingSeconds * 1000L)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
        // Strict mode: only Snooze. Skipping needs the hold on the break screen.
        if (!preferences.strictMode) {
            builder.addAction(
                android.R.drawable.ic_menu_view,
                context.getString(R.string.action_looked_away),
                servicePendingIntent(7, VisionTimerService.ACTION_COMPLETE_BREAK)
            )
        }
        builder.addAction(
            android.R.drawable.ic_lock_idle_alarm,
            context.getString(R.string.action_snooze, preferences.snoozeMinutes),
            servicePendingIntent(6, VisionTimerService.ACTION_SNOOZE)
        )
        return builder.build()
    }

    private fun servicePendingIntent(requestCode: Int, action: String): PendingIntent {
        val intent = Intent(context, VisionTimerService::class.java).apply { this.action = action }
        // Every service action may promote the service to the foreground, which
        // getService() cannot do from the background.
        return PendingIntent.getForegroundService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val CHANNEL_ID_TIMER = "channel_vision_timer"
        const val CHANNEL_ID_BREAK = "channel_vision_break_v2"
        private const val LEGACY_CHANNEL_ID_BREAK = "channel_vision_break"
        const val NOTIFICATION_ID_TIMER = 101
        const val NOTIFICATION_ID_BREAK = 102

        fun formatTime(totalSeconds: Int): String {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }
}
