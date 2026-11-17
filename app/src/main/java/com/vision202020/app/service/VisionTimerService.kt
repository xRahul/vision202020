package com.vision202020.app.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.vision202020.app.data.EnforcementMode
import com.vision202020.app.data.PreferencesRepositoryImpl
import com.vision202020.app.data.TimerProfile
import com.vision202020.app.data.UserPreferences
import com.vision202020.app.data.dataStore
import com.vision202020.app.domain.TimerStatus
import com.vision202020.app.domain.VisionTimerEngine
import com.vision202020.app.overlay.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VisionTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickerJob: Job? = null

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var overlayManager: OverlayManager
    private lateinit var engine: VisionTimerEngine
    private lateinit var screenReceiver: ScreenStateReceiver

    private var currentPrefs = UserPreferences()

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        notificationHelper.createChannels()
        overlayManager = OverlayManager(this)

        val repo = PreferencesRepositoryImpl(applicationContext.dataStore)
        engine = VisionTimerEngine(currentPrefs)

        screenReceiver = ScreenStateReceiver(
            onScreenOn = { engine.onScreenOn() },
            onScreenOff = {
                engine.onScreenOff()
                overlayManager.dismissOverlay()
            }
        )

        serviceScope.launch {
            repo.preferencesFlow.collect { prefs ->
                currentPrefs = prefs
                engine.updatePreferences(prefs)
                if (prefs.timerProfile == TimerProfile.MOBILE_SCREEN_AWARE) {
                    screenReceiver.register(this@VisionTimerService)
                } else {
                    screenReceiver.unregister(this@VisionTimerService)
                }
            }
        }

        serviceScope.launch {
            engine.state.collect { status ->
                _timerStatus.value = status
                handleStateChange(status)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_PAUSE -> engine.pause()
            ACTION_RESUME -> engine.resume()
            ACTION_STOP -> stopTimer()
            ACTION_SNOOZE -> engine.snooze()
            ACTION_TRIGGER_BREAK -> engine.triggerBreak()
            ACTION_COMPLETE_BREAK -> engine.completeBreak()
        }
        return START_STICKY
    }

    private fun startTimer() {
        startForegroundServiceWithNotification()
        engine.start()
        startTicker()
    }

    private fun stopTimer() {
        tickerJob?.cancel()
        engine.stop()
        overlayManager.dismissOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundServiceWithNotification() {
        val initialNotification = notificationHelper.buildTimerNotification(engine.state.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.NOTIFICATION_ID_TIMER,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID_TIMER, initialNotification)
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                engine.tickSecond()
            }
        }
    }

    private fun handleStateChange(status: TimerStatus) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = notificationHelper.buildTimerNotification(status)
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID_TIMER, notification)

        when (status) {
            is TimerStatus.RestBreak -> {
                if (currentPrefs.enforcementMode == EnforcementMode.BLOCKER_OVERLAY && overlayManager.canDrawOverlays()) {
                    overlayManager.showBreakOverlay(
                        remainingSeconds = status.remainingSeconds,
                        onSkip = { engine.completeBreak() },
                        onSnooze = { engine.snooze() }
                    )
                } else {
                    val alertNotification = notificationHelper.buildBreakAlertNotification(status.remainingSeconds)
                    notificationManager.notify(NotificationHelper.NOTIFICATION_ID_BREAK, alertNotification)
                }
            }
            is TimerStatus.Working -> {
                overlayManager.dismissOverlay()
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_BREAK)
            }
            is TimerStatus.Snoozed -> {
                overlayManager.dismissOverlay()
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_BREAK)
            }
            is TimerStatus.Idle -> {
                overlayManager.dismissOverlay()
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_BREAK)
            }
        }
    }

    override fun onDestroy() {
        screenReceiver.unregister(this)
        overlayManager.dismissOverlay()
        serviceScope.cancel()
        _timerStatus.value = TimerStatus.Idle
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.vision202020.action.START"
        const val ACTION_PAUSE = "com.vision202020.action.PAUSE"
        const val ACTION_RESUME = "com.vision202020.action.RESUME"
        const val ACTION_STOP = "com.vision202020.action.STOP"
        const val ACTION_SNOOZE = "com.vision202020.action.SNOOZE"
        const val ACTION_TRIGGER_BREAK = "com.vision202020.action.TRIGGER_BREAK"
        const val ACTION_COMPLETE_BREAK = "com.vision202020.action.COMPLETE_BREAK"

        private val _timerStatus = MutableStateFlow<TimerStatus>(TimerStatus.Idle)
        val timerStatus: StateFlow<TimerStatus> = _timerStatus.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, VisionTimerService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VisionTimerService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }

        fun toggle(context: Context) {
            when (_timerStatus.value) {
                is TimerStatus.Idle -> start(context)
                else -> stop(context)
            }
        }
    }
}
