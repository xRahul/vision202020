package `in`.rahulja.vision202020.tile

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import `in`.rahulja.vision202020.R
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.service.NotificationHelper
import `in`.rahulja.vision202020.service.VisionTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class VisionTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
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
        tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_vision)
        val (state, subtitle) = mapStatusToTileState(this, status)
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
        fun mapStatusToTileState(context: Context, status: TimerStatus): Pair<Int, String> {
            return when (status) {
                is TimerStatus.Idle -> Pair(
                    Tile.STATE_INACTIVE,
                    context.getString(R.string.tile_tap_to_start)
                )
                is TimerStatus.Working -> {
                    val formatted = NotificationHelper.formatTime(status.remainingSeconds)
                    if (status.isScreenPaused) {
                        Pair(Tile.STATE_INACTIVE, context.getString(R.string.tile_paused, formatted))
                    } else {
                        Pair(Tile.STATE_ACTIVE, context.getString(R.string.tile_left, formatted))
                    }
                }
                is TimerStatus.RestBreak -> Pair(
                    Tile.STATE_ACTIVE,
                    context.getString(R.string.tile_rest, status.remainingSeconds)
                )
                is TimerStatus.Snoozed -> Pair(
                    Tile.STATE_INACTIVE,
                    context.getString(
                        R.string.tile_snoozed,
                        NotificationHelper.formatTime(status.remainingSeconds)
                    )
                )
            }
        }
    }
}
