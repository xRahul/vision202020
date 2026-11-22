package `in`.rahulja.vision202020.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import `in`.rahulja.vision202020.R
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.service.NotificationHelper
import `in`.rahulja.vision202020.service.VisionTimerService
import `in`.rahulja.vision202020.ui.theme.VisionGlanceColorProviders

class VisionGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val status = VisionTimerService.timerStatus.value
        val (titleText, buttonText) = when (status) {
            is TimerStatus.Working -> Pair(
                if (status.isScreenPaused) {
                    context.getString(
                        R.string.widget_paused,
                        NotificationHelper.formatTime(status.remainingSeconds)
                    )
                } else {
                    context.getString(
                        R.string.widget_remaining,
                        NotificationHelper.formatTime(status.remainingSeconds)
                    )
                },
                context.getString(
                    if (status.isScreenPaused) R.string.widget_resume else R.string.action_pause
                )
            )
            is TimerStatus.RestBreak -> Pair(
                context.getString(R.string.widget_break, status.remainingSeconds),
                context.getString(R.string.widget_skip)
            )
            is TimerStatus.Snoozed -> Pair(
                context.getString(
                    R.string.widget_snoozed,
                    NotificationHelper.formatTime(status.remainingSeconds)
                ),
                context.getString(R.string.widget_rest_now)
            )
            is TimerStatus.Idle -> Pair(
                context.getString(R.string.widget_idle),
                context.getString(R.string.widget_start)
            )
        }

        provideContent {
            GlanceTheme(colors = VisionGlanceColorProviders) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = context.getString(R.string.widget_title),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = titleText,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Button(
                        text = buttonText,
                        onClick = actionRunCallback<WidgetToggleActionCallback>()
                    )
                }
            }
        }
    }
}

class WidgetToggleActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        VisionTimerService.toggle(context)
    }
}
