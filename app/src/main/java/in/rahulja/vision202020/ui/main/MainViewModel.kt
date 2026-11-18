package `in`.rahulja.vision202020.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.rahulja.vision202020.data.AppThemeMode
import `in`.rahulja.vision202020.data.EnforcementMode
import `in`.rahulja.vision202020.data.PreferencesRepository
import `in`.rahulja.vision202020.data.UserPreferences
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.domain.VisionTimerStateHolder
import `in`.rahulja.vision202020.service.VisionTimerService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: PreferencesRepository
) : ViewModel() {

    val timerStatus: StateFlow<TimerStatus> = VisionTimerStateHolder.state

    val userPreferences: StateFlow<UserPreferences> = repository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun setWorkDuration(minutes: Int) {
        viewModelScope.launch {
            repository.updateWorkDuration(minutes)
        }
    }

    fun setRestDuration(seconds: Int) {
        viewModelScope.launch {
            repository.updateRestDuration(seconds)
        }
    }

    fun setNaturalBreakMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.updateNaturalBreakMinutes(minutes)
        }
    }

    fun setEnforcementMode(mode: EnforcementMode) {
        viewModelScope.launch {
            repository.updateEnforcementMode(mode)
        }
    }

    fun setStrictMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateStrictMode(enabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSoundEnabled(enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateVibrationEnabled(enabled)
        }
    }

    fun setAppTheme(theme: AppThemeMode) {
        viewModelScope.launch {
            repository.updateAppTheme(theme)
        }
    }

    fun setStartOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateStartOnBoot(enabled)
        }
    }
}
