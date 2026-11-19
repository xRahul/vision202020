package `in`.rahulja.vision202020.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import android.util.Log
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import `in`.rahulja.vision202020.data.AppThemeMode
import `in`.rahulja.vision202020.ui.breakui.BreakScreenContent
import `in`.rahulja.vision202020.ui.theme.VisionTheme

class OverlayManager(private val context: Context) {

    private val windowContext: Context =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.createWindowContext(
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    null
                )
            } catch (e: Exception) {
                context
            }
        } else {
            context
        }

    private val windowManager: WindowManager =
        windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null
    private var activeLifecycleOwner: OverlayLifecycleOwner? = null
    private val remainingSecondsState = mutableIntStateOf(20)

    internal var currentLayoutParams: WindowManager.LayoutParams? = null
        private set

    private var _isShowing: Boolean = false
    val isShowing: Boolean get() = _isShowing

    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun showBreakOverlay(
        remainingSeconds: Int,
        totalSeconds: Int = 20,
        strictMode: Boolean = false,
        appTheme: AppThemeMode = AppThemeMode.OLED_BLACK,
        onSkip: () -> Unit,
        onSnooze: () -> Unit
    ) {
        if (!canDrawOverlays() || _isShowing) {
            updateCountdown(remainingSeconds)
            return
        }

        remainingSecondsState.intValue = remainingSeconds

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        activeLifecycleOwner = lifecycleOwner

        val composeView = ComposeView(windowContext).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                VisionTheme(appTheme = appTheme) {
                    BreakScreenContent(
                        remainingSeconds = remainingSecondsState.intValue,
                        totalSeconds = totalSeconds,
                        strictMode = strictMode,
                        onSkip = {
                            dismissOverlay()
                            onSkip()
                        },
                        onSnooze = {
                            dismissOverlay()
                            onSnooze()
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(composeView, layoutParams)
            overlayView = composeView
            currentLayoutParams = layoutParams
            _isShowing = true
        } catch (e: Exception) {
            Log.w("OverlayManager", "Failed to show overlay", e)
            _isShowing = false
            overlayView = null
            currentLayoutParams = null
            activeLifecycleOwner = null
        }
    }

    fun updateCountdown(remainingSeconds: Int) {
        remainingSecondsState.intValue = remainingSeconds
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        if (!_isShowing || overlayView == null) return
        try {
            overlayView?.let { view ->
                windowManager.updateViewLayout(view, view.layoutParams)
            }
        } catch (e: Exception) {
            Log.w("OverlayManager", "Failed to update overlay view layout on config change", e)
        }
    }

    fun dismissOverlay() {
        if (!_isShowing && overlayView == null) return
        try {
            activeLifecycleOwner?.apply {
                handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
            (overlayView as? ComposeView)?.disposeComposition()
            overlayView?.let { windowManager.removeViewImmediate(it) }
        } catch (e: IllegalArgumentException) {
            // Already detached
        } finally {
            overlayView = null
            currentLayoutParams = null
            activeLifecycleOwner = null
            _isShowing = false
        }
    }

    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

        fun performRestore(savedState: android.os.Bundle?) {
            savedStateRegistryController.performRestore(savedState)
        }

        fun handleLifecycleEvent(event: Lifecycle.Event) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }
    }
}
