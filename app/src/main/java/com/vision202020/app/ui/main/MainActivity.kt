package com.vision202020.app.ui.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vision202020.app.data.PreferencesRepositoryImpl
import com.vision202020.app.data.dataStore
import com.vision202020.app.service.VisionTimerService
import com.vision202020.app.ui.settings.SettingsScreen

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = PreferencesRepositoryImpl(applicationContext.dataStore)
        viewModel = MainViewModel(repository)

        setContent {
            val status by viewModel.timerStatus.collectAsStateWithLifecycle()
            val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()

            var hasOverlayPermission by remember {
                mutableStateOf(checkOverlayPermission())
            }

            var isSettingsOpen by remember { mutableStateOf(false) }

            if (isSettingsOpen) {
                SettingsScreen(
                    preferences = preferences,
                    onWorkDurationChanged = { viewModel.setWorkDuration(it) },
                    onRestDurationChanged = { viewModel.setRestDuration(it) },
                    onNaturalBreakChanged = { viewModel.setNaturalBreakMinutes(it) },
                    onTimerProfileChanged = { viewModel.setTimerProfile(it) },
                    onEnforcementModeChanged = { viewModel.setEnforcementMode(it) },
                    onStrictModeChanged = { viewModel.setStrictMode(it) },
                    onSoundChanged = { viewModel.setSoundEnabled(it) },
                    onVibrationChanged = { viewModel.setVibrationEnabled(it) },
                    onBack = { isSettingsOpen = false }
                )
            } else {
                MainScreen(
                    status = status,
                    preferences = preferences,
                    hasOverlayPermission = hasOverlayPermission,
                    onStart = { VisionTimerService.start(this) },
                    onPause = { sendServiceAction(VisionTimerService.ACTION_PAUSE) },
                    onResume = { sendServiceAction(VisionTimerService.ACTION_RESUME) },
                    onStop = { VisionTimerService.stop(this) },
                    onTriggerBreak = { sendServiceAction(VisionTimerService.ACTION_TRIGGER_BREAK) },
                    onOpenSettings = { isSettingsOpen = true },
                    onPermissionRequested = {
                        hasOverlayPermission = checkOverlayPermission()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning from system settings
        checkOverlayPermission()
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun sendServiceAction(actionName: String) {
        val intent = Intent(this, VisionTimerService::class.java).apply { action = actionName }
        startService(intent)
    }
}
