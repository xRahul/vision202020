package com.vision202020.app.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.vision202020.app.domain.TimerStatus
import com.vision202020.app.service.NotificationHelper
import com.vision202020.app.service.VisionTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class VisionTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var observerJob: Job? = null
    private var lastState: Int? = null
    private var lastSubtitle: String? = null

    override fun onStartListening() {
        super.onStartListening()
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            VisionTimerService.timerStatus.collect { status ->
                updateTile(status)
            }
        }
    }

    override fun onStopListening() {
        observerJob?.cancel()
        lastState = null
        lastSubtitle = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        VisionTimerService.toggle(applicationContext)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun updateTile(status: TimerStatus) {
        val tile = qsTile ?: return
        val (state, subtitle) = mapStatusToTileState(status)
        if (state != lastState || subtitle != lastSubtitle) {
            lastState = state
            lastSubtitle = subtitle
            tile.state = state
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = subtitle
            }
            tile.updateTile()
        }
    }

    companion object {
        fun mapStatusToTileState(status: TimerStatus): Pair<Int, String> {
            return when (status) {
                is TimerStatus.Idle -> {
                    Pair(Tile.STATE_INACTIVE, "Tap to start")
                }
                is TimerStatus.Working -> {
                    val formatted = NotificationHelper.formatTime(status.remainingSeconds)
                    if (status.isScreenPaused) {
                        Pair(Tile.STATE_INACTIVE, "Paused ($formatted)")
                    } else {
                        Pair(Tile.STATE_ACTIVE, "$formatted left")
                    }
                }
                is TimerStatus.RestBreak -> {
                    Pair(Tile.STATE_ACTIVE, "Rest: ${status.remainingSeconds}s")
                }
                is TimerStatus.Snoozed -> {
                    val formatted = NotificationHelper.formatTime(status.remainingSeconds)
                    Pair(Tile.STATE_INACTIVE, "Snoozed ($formatted)")
                }
            }
        }
    }
}
