package `in`.rahulja.vision202020.ui.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.service.NotificationHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PermissionStateTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    private fun createBreakChannel(importance: Int) {
        manager.createNotificationChannel(
            NotificationChannel(NotificationHelper.CHANNEL_ID_BREAK, "Break", importance)
        )
    }

    @Test
    @Config(sdk = [26])
    fun notificationsDisabledOnApi26IsReported() {
        shadowOf(manager).setNotificationsEnabled(false)

        assertFalse(readPermissionState(context).notifications)
    }

    @Test
    @Config(sdk = [26])
    fun notificationsEnabledIsReported() {
        shadowOf(manager).setNotificationsEnabled(true)

        assertTrue(readPermissionState(context).notifications)
    }

    @Test
    @Config(sdk = [26])
    fun blockedBreakChannelIsReported() {
        createBreakChannel(NotificationManager.IMPORTANCE_NONE)

        assertFalse(readPermissionState(context).breakChannelEnabled)
    }

    @Test
    @Config(sdk = [26])
    fun enabledOrMissingBreakChannelIsReportedEnabled() {
        assertTrue(readPermissionState(context).breakChannelEnabled)

        createBreakChannel(NotificationManager.IMPORTANCE_HIGH)

        assertTrue(readPermissionState(context).breakChannelEnabled)
    }

    @Test
    @Config(sdk = [26])
    fun batteryOptimizationIgnoredIsReported() {
        val power = context.getSystemService(PowerManager::class.java)
        shadowOf(power).setIgnoringBatteryOptimizations(context.packageName, true)
        assertTrue(readPermissionState(context).ignoringBatteryOptimizations)

        shadowOf(power).setIgnoringBatteryOptimizations(context.packageName, false)
        assertFalse(readPermissionState(context).ignoringBatteryOptimizations)
    }

    @Test
    fun permanentDenialOpensSettingsOnlyWithoutRationale() {
        assertTrue(shouldOpenNotificationSettings(granted = false, showRationale = false))
        assertFalse(shouldOpenNotificationSettings(granted = false, showRationale = true))
        assertFalse(shouldOpenNotificationSettings(granted = true, showRationale = false))
    }
}
