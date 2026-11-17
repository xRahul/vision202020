package com.vision202020.app.data

enum class EnforcementMode {
    BLOCKER_OVERLAY,
    NOTIFICATION_ONLY
}

data class UserPreferences(
    val workDurationMinutes: Int = 20,
    val restDurationSeconds: Int = 20,
    val naturalBreakMinutes: Int = 5,
    val enforcementMode: EnforcementMode = EnforcementMode.BLOCKER_OVERLAY,
    val strictMode: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)
