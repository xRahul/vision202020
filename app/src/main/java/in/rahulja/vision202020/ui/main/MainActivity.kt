package `in`.rahulja.vision202020.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.dataStore
import `in`.rahulja.vision202020.service.VisionTimerService
import `in`.rahulja.vision202020.ui.settings.SettingsScreen
import `in`.rahulja.vision202020.ui.theme.VisionTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(
                        PreferencesRepositoryImpl(applicationContext.dataStore)
                    ) as T
                }
            }
        }
    )

    private var hasOverlayPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)
    private var isSettingsOpen by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        refreshPermissionStates()

        setContent {
            val status by viewModel.timerStatus.collectAsStateWithLifecycle()
            val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasNotificationPermission = isGranted
            }

            VisionTheme(appTheme = preferences.appTheme) {
                if (isSettingsOpen) {
                    SettingsScreen(
                        preferences = preferences,
                        onWorkDurationChanged = { viewModel.setWorkDuration(it) },
                        onRestDurationChanged = { viewModel.setRestDuration(it) },
                        onNaturalBreakChanged = { viewModel.setNaturalBreakMinutes(it) },
                        onEnforcementModeChanged = { viewModel.setEnforcementMode(it) },
                        onStrictModeChanged = { viewModel.setStrictMode(it) },
                        onSoundChanged = { viewModel.setSoundEnabled(it) },
                        onVibrationChanged = { viewModel.setVibrationEnabled(it) },
                        onAppThemeChanged = { viewModel.setAppTheme(it) },
                        onStartOnBootChanged = { viewModel.setStartOnBoot(it) },
                        onBack = { isSettingsOpen = false }
                    )
                } else {
                    MainScreen(
                        status = status,
                        preferences = preferences,
                        hasOverlayPermission = hasOverlayPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        onStart = { VisionTimerService.start(this) },
                        onPause = { sendServiceAction(VisionTimerService.ACTION_PAUSE) },
                        onResume = { sendServiceAction(VisionTimerService.ACTION_RESUME) },
                        onStop = { VisionTimerService.stop(this) },
                        onTriggerBreak = { sendServiceAction(VisionTimerService.ACTION_TRIGGER_BREAK) },
                        onOpenSettings = { isSettingsOpen = true },
                        onRequestOverlayPermission = {
                            refreshPermissionStates()
                        },
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
    }

    private fun refreshPermissionStates() {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun sendServiceAction(actionName: String) {
        val intent = Intent(this, VisionTimerService::class.java).apply { action = actionName }
        startService(intent)
    }
}
