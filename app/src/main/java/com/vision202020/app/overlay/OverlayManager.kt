package com.vision202020.app.overlay

import android.content.Context
import android.os.Build
import android.provider.Settings

class OverlayManager(private val context: Context) {

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
        onSkip: () -> Unit,
        onSnooze: () -> Unit
    ) {
        if (!canDrawOverlays()) return
        _isShowing = true
    }

    fun updateCountdown(remainingSeconds: Int) {
        // Updated in Task 5
    }

    fun dismissOverlay() {
        _isShowing = false
    }
}
