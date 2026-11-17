package com.vision202020.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vision202020.app.data.EnforcementMode
import com.vision202020.app.data.PreferencesRepository
import com.vision202020.app.data.TimerProfile
import com.vision202020.app.data.UserPreferences
import com.vision202020.app.domain.TimerStatus
import com.vision202020.app.service.VisionTimerService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: PreferencesRepository
) : ViewModel() {

    val timerStatus: StateFlow<TimerStatus> = VisionTimerService.timerStatus

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

    fun setTimerProfile(profile: TimerProfile) {
        viewModelScope.launch {
            repository.updateTimerProfile(profile)
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
}
