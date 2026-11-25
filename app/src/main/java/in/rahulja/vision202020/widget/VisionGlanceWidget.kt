package `in`.rahulja.vision202020.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
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
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.dataStore
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.service.VisionTimerService
import `in`.rahulja.vision202020.ui.breakui.BreakLauncher
import `in`.rahulja.vision202020.ui.main.MainActivity
import `in`.rahulja.vision202020.ui.text.TimerStatusText
import `in`.rahulja.vision202020.ui.theme.VisionGlanceColorProviders
import kotlinx.coroutines.flow.first

class VisionGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefsFlow = PreferencesRepositoryImpl(context.dataStore).preferencesFlow
        // Read the real strict flag up front so the first composition never assumes non-strict.
        val initialPrefs = prefsFlow.first()

        provideContent {
            val status by VisionTimerService.timerStatus.collectAsState()
            val prefs by prefsFlow.collectAsState(initial = initialPrefs)
            val strictMode = prefs.strictMode
            // Strict mode: a tap during a break must not skip, so it opens the break screen instead.
            val buttonAction = if (strictMode && status is TimerStatus.RestBreak) {
                actionStartActivity(BreakLauncher.intent(context))
            } else {
                actionRunCallback<WidgetToggleActionCallback>()
            }
            val showTitleRow = LocalSize.current.height >= LARGE.height
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
                    if (showTitleRow) {
                        Text(
                            text = context.getString(R.string.widget_title),
                            modifier = GlanceModifier.clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }
                    Text(
                        text = TimerStatusText.title(context, status),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Button(
                        text = TimerStatusText.actionLabel(context, status, strictMode),
                        onClick = buttonAction
                    )
                }
            }
        }
    }

    private companion object {
        val SMALL = DpSize(110.dp, 70.dp)
        val LARGE = DpSize(180.dp, 110.dp)
    }
}

class WidgetToggleActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        VisionTimerService.toggle(context)
    }
}
