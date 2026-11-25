package `in`.rahulja.vision202020.ui

import android.app.Application
import android.app.NotificationManager
import android.provider.Settings
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.ui.main.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h800dp-port")
class MainActivityNotificationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun denyNotifications() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
    }

    @Test
    fun secondTapAfterDeniedRequestOpensAppNotificationSettings() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = shadowOf(app)
        composeTestRule.activityRule.scenario.onActivity { it.recreate() }
        composeTestRule.waitForIdle()

        // First tap: system permission dialog is requested, not the settings screen.
        composeTestRule.onNodeWithText("Enable Notifications").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        while (shadowApp.nextStartedActivity != null) { /* drain permission request */ }

        // Second tap: request was made and no rationale remains, so settings must open.
        composeTestRule.onNodeWithText("Enable Notifications").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        val started = shadowApp.nextStartedActivity
        assertNotNull("settings activity should be started", started)
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, started.action)
        assertEquals(app.packageName, started.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }
}
