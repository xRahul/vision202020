package `in`.rahulja.vision202020.data

enum class AppThemeMode {
    LIGHT,
    DARK,
    OLED_BLACK
}

enum class EnforcementMode {
    BLOCKER_OVERLAY,
    NOTIFICATION_ONLY
}

/** Bounds of the options Settings offers; the repository clamps to them at read and write. */
object PreferenceLimits {
    val WORK_MINUTES = 10..30
    val REST_SECONDS = 15..60
    val NATURAL_BREAK_MINUTES = 2..15
    val SNOOZE_MINUTES = 1..10
}

data class UserPreferences(
    val workDurationMinutes: Int = 20,
    val restDurationSeconds: Int = 20,
    val naturalBreakMinutes: Int = 5,
    val snoozeMinutes: Int = 5,
    val enforcementMode: EnforcementMode = EnforcementMode.BLOCKER_OVERLAY,
    val strictMode: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val appTheme: AppThemeMode = AppThemeMode.OLED_BLACK,
    val startOnBoot: Boolean = false
)
