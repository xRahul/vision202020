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
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.service.NotificationHelper
import `in`.rahulja.vision202020.service.VisionTimerService

class VisionGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val status = VisionTimerService.timerStatus.value
        val (titleText, buttonText) = when (status) {
            is TimerStatus.Working -> {
                val formatted = NotificationHelper.formatTime(status.remainingSeconds)
                val label = if (status.isScreenPaused) "Paused: $formatted" else "$formatted remaining"
                Pair(label, "Stop")
            }
            is TimerStatus.RestBreak -> {
                Pair("Rest eyes: ${status.remainingSeconds}s", "Skip")
            }
            is TimerStatus.Snoozed -> {
                val formatted = NotificationHelper.formatTime(status.remainingSeconds)
                Pair("Snoozed: $formatted", "Stop")
            }
            is TimerStatus.Idle -> {
                Pair("20-20-20 Ready", "Start")
            }
        }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Vision 20-20-20",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = titleText,
                        style = TextStyle(
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
        val currentStatus = VisionTimerService.timerStatus.value
        when (currentStatus) {
            is TimerStatus.Idle -> VisionTimerService.start(context)
            is TimerStatus.RestBreak -> {
                val intent = android.content.Intent(context, VisionTimerService::class.java).apply {
                    action = VisionTimerService.ACTION_COMPLETE_BREAK
                }
                context.startService(intent)
            }
            else -> VisionTimerService.stop(context)
        }
        VisionGlanceWidget().update(context, glanceId)
    }
}
