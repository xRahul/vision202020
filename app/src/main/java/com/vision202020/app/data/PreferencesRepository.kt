package com.vision202020.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vision_preferences")

interface PreferencesRepository {
    val preferencesFlow: Flow<UserPreferences>
    suspend fun updateWorkDuration(minutes: Int)
    suspend fun updateRestDuration(seconds: Int)
    suspend fun updateNaturalBreakMinutes(minutes: Int)
    suspend fun updateEnforcementMode(mode: EnforcementMode)
    suspend fun updateStrictMode(enabled: Boolean)
    suspend fun updateSoundEnabled(enabled: Boolean)
    suspend fun updateVibrationEnabled(enabled: Boolean)
    suspend fun updateAppTheme(theme: AppThemeMode)
}

class PreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    private object PreferencesKeys {
        val WORK_DURATION_MINUTES = intPreferencesKey("work_duration_minutes")
        val REST_DURATION_SECONDS = intPreferencesKey("rest_duration_seconds")
        val NATURAL_BREAK_MINUTES = intPreferencesKey("natural_break_minutes")
        val ENFORCEMENT_MODE = stringPreferencesKey("enforcement_mode")
        val STRICT_MODE = booleanPreferencesKey("strict_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val APP_THEME = stringPreferencesKey("app_theme")
    }

    override val preferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val workMin = preferences[PreferencesKeys.WORK_DURATION_MINUTES] ?: 20
            val restSec = preferences[PreferencesKeys.REST_DURATION_SECONDS] ?: 20
            val naturalBreakMin = preferences[PreferencesKeys.NATURAL_BREAK_MINUTES] ?: 5
            val modeStr = preferences[PreferencesKeys.ENFORCEMENT_MODE] ?: EnforcementMode.BLOCKER_OVERLAY.name
            val strict = preferences[PreferencesKeys.STRICT_MODE] ?: false
            val sound = preferences[PreferencesKeys.SOUND_ENABLED] ?: true
            val vibe = preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true
            val themeStr = preferences[PreferencesKeys.APP_THEME] ?: AppThemeMode.OLED_BLACK.name

            val mode = try {
                EnforcementMode.valueOf(modeStr)
            } catch (e: IllegalArgumentException) {
                EnforcementMode.BLOCKER_OVERLAY
            }

            val theme = try {
                AppThemeMode.valueOf(themeStr)
            } catch (e: IllegalArgumentException) {
                AppThemeMode.OLED_BLACK
            }

            UserPreferences(
                workDurationMinutes = workMin,
                restDurationSeconds = restSec,
                naturalBreakMinutes = naturalBreakMin,
                enforcementMode = mode,
                strictMode = strict,
                soundEnabled = sound,
                vibrationEnabled = vibe,
                appTheme = theme
            )
        }

    override suspend fun updateWorkDuration(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WORK_DURATION_MINUTES] = minutes
        }
    }

    override suspend fun updateRestDuration(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REST_DURATION_SECONDS] = seconds
        }
    }

    override suspend fun updateNaturalBreakMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NATURAL_BREAK_MINUTES] = minutes
        }
    }

    override suspend fun updateEnforcementMode(mode: EnforcementMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENFORCEMENT_MODE] = mode.name
        }
    }

    override suspend fun updateStrictMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.STRICT_MODE] = enabled
        }
    }

    override suspend fun updateSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SOUND_ENABLED] = enabled
        }
    }

    override suspend fun updateVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIBRATION_ENABLED] = enabled
        }
    }

    override suspend fun updateAppTheme(theme: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_THEME] = theme.name
        }
    }
}
