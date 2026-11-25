package `in`.rahulja.vision202020.ui.main

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import `in`.rahulja.vision202020.service.NotificationHelper

data class PermissionState(
    val overlay: Boolean,
    val notifications: Boolean,
    val breakChannelEnabled: Boolean,
    val fullScreenIntent: Boolean,
    val ignoringBatteryOptimizations: Boolean
)

fun readPermissionState(context: Context): PermissionState {
    val notificationManager = NotificationManagerCompat.from(context)
    val breakChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID_BREAK)
    val fullScreenIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() ?: true
    } else {
        true
    }
    val power = context.getSystemService(PowerManager::class.java)
    return PermissionState(
        overlay = Settings.canDrawOverlays(context),
        notifications = notificationManager.areNotificationsEnabled(),
        // A channel that does not exist yet cannot be blocked.
        breakChannelEnabled = breakChannel == null ||
            breakChannel.importance != NotificationManagerCompat.IMPORTANCE_NONE,
        fullScreenIntent = fullScreenIntent,
        ignoringBatteryOptimizations = power?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    )
}

/** After a denial with no rationale left, the system dialog will never show again. */
fun shouldOpenNotificationSettings(granted: Boolean, showRationale: Boolean): Boolean = !granted && !showRationale

fun appNotificationSettingsIntent(context: Context): Intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

fun breakChannelSettingsIntent(context: Context): Intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    .putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_ID_BREAK)

fun batterySettingsIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

fun appDetailsSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())

/** Starts [primary]; falls back to the app details screen when OEM builds lack the target. */
fun Context.startSettingsWithFallback(primary: Intent) {
    val started = runCatching { startActivity(primary) }.isSuccess
    if (!started) runCatching { startActivity(appDetailsSettingsIntent(this)) }
}
