package `in`.rahulja.vision202020.ui.main

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
    private var canUseFullScreenIntent by mutableStateOf(true)
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

            BackHandler(enabled = isSettingsOpen) {
                isSettingsOpen = false
            }

            VisionTheme(appTheme = preferences.appTheme) {
                if (isSettingsOpen) {
                    SettingsScreen(
                        preferences = preferences,
                        onWorkDurationChanged = { viewModel.setWorkDuration(it) },
                        onRestDurationChanged = { viewModel.setRestDuration(it) },
                        onNaturalBreakChanged = { viewModel.setNaturalBreakMinutes(it) },
                        onSnoozeChanged = { viewModel.setSnoozeMinutes(it) },
                        onEnforcementModeChanged = { viewModel.setEnforcementMode(it) },
                        onStrictModeChanged = { viewModel.setStrictMode(it) },
                        onSoundChanged = { viewModel.setSoundEnabled(it) },
                        onVibrationChanged = { viewModel.setVibrationEnabled(it) },
                        onStartOnBootChanged = { viewModel.setStartOnBoot(it) },
                        onAppThemeChanged = { viewModel.setAppTheme(it) },
                        onBack = { isSettingsOpen = false }
                    )
                } else {
                    MainScreen(
                        status = status,
                        preferences = preferences,
                        hasOverlayPermission = hasOverlayPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        showFullScreenIntentCard = !canUseFullScreenIntent,
                        onStart = { VisionTimerService.start(this) },
                        onPause = { VisionTimerService.pause(this) },
                        onResume = { VisionTimerService.resume(this) },
                        onReset = { VisionTimerService.reset(this) },
                        onStop = { VisionTimerService.stop(this) },
                        onTriggerBreak = { VisionTimerService.triggerBreak(this) },
                        onOpenSettings = { isSettingsOpen = true },
                        onRequestOverlayPermission = { refreshPermissionStates() },
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onRequestFullScreenIntent = { refreshPermissionStates() }
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
        hasOverlayPermission = Settings.canDrawOverlays(this)

        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        canUseFullScreenIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.canUseFullScreenIntent() ?: true
        } else {
            true
        }
    }
}
