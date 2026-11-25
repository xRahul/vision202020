package `in`.rahulja.vision202020.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vision_preferences")

interface PreferencesRepository {
    val preferencesFlow: Flow<UserPreferences>
    suspend fun updateWorkDuration(minutes: Int)
    suspend fun updateRestDuration(seconds: Int)
    suspend fun updateNaturalBreakMinutes(minutes: Int)
    suspend fun updateSnoozeMinutes(minutes: Int)
    suspend fun updateEnforcementMode(mode: EnforcementMode)
    suspend fun updateStrictMode(enabled: Boolean)
    suspend fun updateSoundEnabled(enabled: Boolean)
    suspend fun updateVibrationEnabled(enabled: Boolean)
    suspend fun updateAppTheme(theme: AppThemeMode)
    suspend fun updateStartOnBoot(enabled: Boolean)
}

class PreferencesRepositoryImpl(private val dataStore: DataStore<Preferences>) : PreferencesRepository {

    private object PreferencesKeys {
        val WORK_DURATION_MINUTES = intPreferencesKey("work_duration_minutes")
        val REST_DURATION_SECONDS = intPreferencesKey("rest_duration_seconds")
        val NATURAL_BREAK_MINUTES = intPreferencesKey("natural_break_minutes")
        val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
        val ENFORCEMENT_MODE = stringPreferencesKey("enforcement_mode")
        val STRICT_MODE = booleanPreferencesKey("strict_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val APP_THEME = stringPreferencesKey("app_theme")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
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
            val workMin = (preferences[PreferencesKeys.WORK_DURATION_MINUTES] ?: 20).coerceIn(PreferenceLimits.WORK_MINUTES)
            val restSec = (preferences[PreferencesKeys.REST_DURATION_SECONDS] ?: 20).coerceIn(PreferenceLimits.REST_SECONDS)
            val naturalBreakMin = (preferences[PreferencesKeys.NATURAL_BREAK_MINUTES] ?: 5).coerceIn(PreferenceLimits.NATURAL_BREAK_MINUTES)
            val snoozeMin = (preferences[PreferencesKeys.SNOOZE_MINUTES] ?: 5).coerceIn(PreferenceLimits.SNOOZE_MINUTES)
            val modeStr = preferences[PreferencesKeys.ENFORCEMENT_MODE] ?: EnforcementMode.BLOCKER_OVERLAY.name
            val strict = preferences[PreferencesKeys.STRICT_MODE] ?: false
            val sound = preferences[PreferencesKeys.SOUND_ENABLED] ?: true
            val vibe = preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true
            val themeStr = preferences[PreferencesKeys.APP_THEME] ?: AppThemeMode.OLED_BLACK.name
            val startOnBoot = preferences[PreferencesKeys.START_ON_BOOT] ?: false

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
                snoozeMinutes = snoozeMin,
                enforcementMode = mode,
                strictMode = strict,
                soundEnabled = sound,
                vibrationEnabled = vibe,
                appTheme = theme,
                startOnBoot = startOnBoot
            )
        }

    override suspend fun updateWorkDuration(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WORK_DURATION_MINUTES] = minutes.coerceIn(PreferenceLimits.WORK_MINUTES)
        }
    }

    override suspend fun updateRestDuration(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REST_DURATION_SECONDS] = seconds.coerceIn(PreferenceLimits.REST_SECONDS)
        }
    }

    override suspend fun updateNaturalBreakMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NATURAL_BREAK_MINUTES] = minutes.coerceIn(PreferenceLimits.NATURAL_BREAK_MINUTES)
        }
    }

    override suspend fun updateSnoozeMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SNOOZE_MINUTES] = minutes.coerceIn(PreferenceLimits.SNOOZE_MINUTES)
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

    override suspend fun updateStartOnBoot(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.START_ON_BOOT] = enabled
        }
    }
}
