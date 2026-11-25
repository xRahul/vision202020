package `in`.rahulja.vision202020.ui.text

import android.content.Context
import `in`.rahulja.vision202020.R
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.service.NotificationHelper

/** Single source of status-to-text mapping for glanceable surfaces such as the widget. */
object TimerStatusText {

    fun title(context: Context, status: TimerStatus): String = when (status) {
        is TimerStatus.Working -> context.getString(
            if (status.isScreenPaused) R.string.widget_paused else R.string.widget_remaining,
            NotificationHelper.formatTime(status.remainingSeconds)
        )

        is TimerStatus.RestBreak -> context.getString(R.string.widget_break, status.remainingSeconds)

        is TimerStatus.Snoozed -> context.getString(
            R.string.widget_snoozed,
            NotificationHelper.formatTime(status.remainingSeconds)
        )

        is TimerStatus.Idle -> context.getString(R.string.widget_idle)
    }

    fun actionLabel(context: Context, status: TimerStatus, strictMode: Boolean): String = context.getString(
        when (status) {
            is TimerStatus.Working -> if (status.isScreenPaused) R.string.widget_resume else R.string.action_pause

            // Strict mode never skips from a tap; the button opens the break screen instead.
            is TimerStatus.RestBreak -> if (strictMode) R.string.widget_open_break else R.string.widget_skip

            is TimerStatus.Snoozed -> R.string.widget_rest_now

            is TimerStatus.Idle -> R.string.widget_start
        }
    )
}
