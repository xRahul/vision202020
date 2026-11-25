package `in`.rahulja.vision202020.ui.main

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
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
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = MainViewModel(
                    PreferencesRepositoryImpl(applicationContext.dataStore)
                ) as T
            }
        }
    )

    private var permissionState by mutableStateOf(PermissionState(false, true, true, true, true))
    private var isSettingsOpen by mutableStateOf(false)
    private var notificationRequestedBefore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        refreshPermissionStates()

        setContent {
            val status by viewModel.timerStatus.collectAsStateWithLifecycle()
            val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                refreshPermissionStates()
                if (shouldOpenNotificationSettings(isGranted, shouldShowNotificationRationale())) {
                    startSettingsWithFallback(appNotificationSettingsIntent(this))
                }
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
                        hasOverlayPermission = permissionState.overlay,
                        hasNotificationPermission = permissionState.notifications,
                        showFullScreenIntentCard = !permissionState.fullScreenIntent,
                        showBreakChannelCard = !permissionState.breakChannelEnabled,
                        showBatteryCard = !permissionState.ignoringBatteryOptimizations,
                        onStart = { VisionTimerService.start(this) },
                        onPause = { VisionTimerService.pause(this) },
                        onResume = { VisionTimerService.resume(this) },
                        onReset = { VisionTimerService.reset(this) },
                        onStop = { VisionTimerService.stop(this) },
                        onTriggerBreak = { VisionTimerService.triggerBreak(this) },
                        onOpenSettings = { isSettingsOpen = true },
                        onRequestOverlayPermission = { refreshPermissionStates() },
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                notificationRequestedBefore && !shouldShowNotificationRationale()
                            ) {
                                startSettingsWithFallback(appNotificationSettingsIntent(this))
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationRequestedBefore = true
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                // Pre-13: notifications are blocked in system settings only.
                                startSettingsWithFallback(appNotificationSettingsIntent(this))
                            }
                        },
                        onRequestFullScreenIntent = { refreshPermissionStates() },
                        onOpenBreakChannelSettings = { startSettingsWithFallback(breakChannelSettingsIntent(this)) },
                        onRequestBatteryExemption = { startSettingsWithFallback(batterySettingsIntent()) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
    }

    private fun shouldShowNotificationRationale(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)

    private fun refreshPermissionStates() {
        permissionState = readPermissionState(this)
    }
}
