package `in`.rahulja.vision202020.ui.breakui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.UserPreferences
import `in`.rahulja.vision202020.data.dataStore
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.service.VisionTimerService
import `in`.rahulja.vision202020.ui.theme.VisionTheme

class BreakActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureLockScreenDisplay()

        // The break screen is a blocker: system Back must not dismiss it.
        onBackPressedDispatcher.addCallback(this) {
            // Intentionally swallowed. Skip requires the visible Skip / hold-to-skip control.
        }

        val repository = PreferencesRepositoryImpl(applicationContext.dataStore)

        setContent {
            val status by VisionTimerService.timerStatus.collectAsStateWithLifecycle()
            val preferences by repository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = UserPreferences())
            // Only finish after a break was actually observed: a cold process starts at
            // Idle and must not close the screen before the service restores its state.
            var sawBreak by remember { mutableStateOf(status is TimerStatus.RestBreak) }
            LaunchedEffect(Unit) {
                // Cold start on a stale alert: never leave a blocker screen with no break.
                kotlinx.coroutines.delay(2000)
                if (!sawBreak) finish()
            }
            LaunchedEffect(status) {
                if (status is TimerStatus.RestBreak) {
                    sawBreak = true
                } else if (sawBreak) {
                    finish()
                }
            }
            val breakStatus = status as? TimerStatus.RestBreak
            val remainingSec = breakStatus?.remainingSeconds ?: preferences.restDurationSeconds
            val totalSec = breakStatus?.totalSeconds ?: preferences.restDurationSeconds

            VisionTheme(appTheme = preferences.appTheme) {
                BreakScreenContent(
                    remainingSeconds = remainingSec,
                    totalSeconds = totalSec,
                    strictMode = preferences.strictMode,
                    snoozeMinutes = preferences.snoozeMinutes,
                    onSkip = {
                        if (breakStatus != null) VisionTimerService.completeBreak(this, fromBreakUi = true)
                        finish()
                    },
                    onSnooze = {
                        if (breakStatus != null) VisionTimerService.snooze(this)
                        finish()
                    }
                )
            }
        }
    }

    private fun configureLockScreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
}
