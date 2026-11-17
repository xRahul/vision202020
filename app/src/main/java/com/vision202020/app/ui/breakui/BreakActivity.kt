package com.vision202020.app.ui.breakui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vision202020.app.domain.TimerStatus
import com.vision202020.app.service.VisionTimerService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BreakActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureLockScreenDisplay()

        setContent {
            val status = VisionTimerService.timerStatus.value
            val remainingSec = if (status is TimerStatus.RestBreak) status.remainingSeconds else 20
            val totalSec = if (status is TimerStatus.RestBreak) status.totalSeconds else 20

            BreakScreenContent(
                remainingSeconds = remainingSec,
                totalSeconds = totalSec,
                strictMode = false,
                onSkip = {
                    sendServiceAction(VisionTimerService.ACTION_COMPLETE_BREAK)
                    finish()
                },
                onSnooze = {
                    sendServiceAction(VisionTimerService.ACTION_SNOOZE)
                    finish()
                }
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                VisionTimerService.timerStatus.collectLatest { status ->
                    if (status !is TimerStatus.RestBreak) {
                        finish()
                    }
                }
            }
        }
    }

    private fun configureLockScreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun sendServiceAction(actionName: String) {
        val intent = Intent(this, VisionTimerService::class.java).apply { action = actionName }
        startService(intent)
    }
}
