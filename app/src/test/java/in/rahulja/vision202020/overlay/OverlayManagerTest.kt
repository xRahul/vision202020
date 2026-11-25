package `in`.rahulja.vision202020.overlay

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
class OverlayManagerTest {

    private lateinit var context: Context
    private lateinit var overlayManager: OverlayManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        overlayManager = OverlayManager(context)
    }

    @Test
    fun testOverlayPermissionCheck() {
        ShadowSettings.setCanDrawOverlays(false)
        assertFalse("Should be false when overlay permission not granted", overlayManager.canDrawOverlays())

        ShadowSettings.setCanDrawOverlays(true)
        assertTrue("Should be true when overlay permission granted", overlayManager.canDrawOverlays())
    }

    @Test
    fun testShowAndDismissOverlay() {
        ShadowSettings.setCanDrawOverlays(true)

        assertFalse(overlayManager.isShowing)

        overlayManager.showBreakOverlay(
            remainingSeconds = 20,
            onSkip = {},
            onSnooze = {}
        )
        assertTrue(overlayManager.isShowing)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            assertEquals(
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES,
                overlayManager.currentLayoutParams?.layoutInDisplayCutoutMode
            )
        }

        overlayManager.dismissOverlay()
        assertFalse(overlayManager.isShowing)

        // Idempotent dismiss
        overlayManager.dismissOverlay()
        assertFalse(overlayManager.isShowing)
    }

    @Test
    fun testShowOverlayWithoutPermissionDoesNotShow() {
        ShadowSettings.setCanDrawOverlays(false)

        overlayManager.showBreakOverlay(
            remainingSeconds = 20,
            onSkip = {},
            onSnooze = {}
        )
        assertFalse("Overlay must not show without permission", overlayManager.isShowing)
    }

    @Test
    fun testOnConfigurationChangedWhenShowingAndNotShowing() {
        ShadowSettings.setCanDrawOverlays(true)

        // When not showing, calling onConfigurationChanged is a safe no-op
        val config = android.content.res.Configuration()
        overlayManager.onConfigurationChanged(config)

        // Show overlay
        overlayManager.showBreakOverlay(
            remainingSeconds = 20,
            onSkip = {},
            onSnooze = {}
        )
        assertTrue(overlayManager.isShowing)

        // When showing, calling onConfigurationChanged updates view layout smoothly
        overlayManager.onConfigurationChanged(config)
        assertTrue(overlayManager.isShowing)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            assertEquals(
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES,
                overlayManager.currentLayoutParams?.layoutInDisplayCutoutMode
            )
        }

        overlayManager.dismissOverlay()
        assertFalse(overlayManager.isShowing)
    }
}
