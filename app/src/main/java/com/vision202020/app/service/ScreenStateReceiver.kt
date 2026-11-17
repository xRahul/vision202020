package com.vision202020.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class ScreenStateReceiver(
    private val onScreenOn: () -> Unit,
    private val onScreenOff: () -> Unit
) : BroadcastReceiver() {

    private var isRegistered = false

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_ON -> onScreenOn()
            Intent.ACTION_SCREEN_OFF -> onScreenOff()
        }
    }

    fun register(context: Context) {
        if (!isRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            context.registerReceiver(this, filter)
            isRegistered = true
        }
    }

    fun unregister(context: Context) {
        if (isRegistered) {
            try {
                context.unregisterReceiver(this)
            } catch (e: IllegalArgumentException) {
                // Already unregistered
            }
            isRegistered = false
        }
    }
}
