package com.vision202020.app.overlay

import android.content.Context
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
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.vision202020.app.ui.breakui.BreakScreenContent

class OverlayManager(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null
    private val remainingSecondsState = mutableIntStateOf(20)

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
        }

        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
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

        try {
            windowManager.addView(composeView, layoutParams)
            overlayView = composeView
            _isShowing = true
        } catch (e: Exception) {
            _isShowing = false
            overlayView = null
        }
    }

    fun updateCountdown(remainingSeconds: Int) {
        remainingSecondsState.intValue = remainingSeconds
    }

    fun dismissOverlay() {
        if (!_isShowing || overlayView == null) return
        try {
            overlayView?.let { windowManager.removeViewImmediate(it) }
        } catch (e: IllegalArgumentException) {
            // Already detached
        } finally {
            overlayView = null
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
