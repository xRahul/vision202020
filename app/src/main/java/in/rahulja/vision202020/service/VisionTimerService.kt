package `in`.rahulja.vision202020.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import `in`.rahulja.vision202020.data.EnforcementMode
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.UserPreferences
import `in`.rahulja.vision202020.data.dataStore
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.domain.VisionTimerEngine
import `in`.rahulja.vision202020.domain.VisionTimerStateHolder
import `in`.rahulja.vision202020.overlay.OverlayManager
import androidx.glance.appwidget.updateAll
import `in`.rahulja.vision202020.widget.VisionGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VisionTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickerJob: Job? = null

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var overlayManager: OverlayManager
    private lateinit var soundAlertHelper: SoundAlertHelper
    private lateinit var engine: VisionTimerEngine
    private lateinit var screenReceiver: ScreenStateReceiver

    private var currentPrefs = UserPreferences()
    private var previousStatus: TimerStatus? = null
    private var lastWidgetUpdateMs: Long = 0L
    private var lastTimerNotificationKey: String? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        notificationHelper.createChannels()
        overlayManager = OverlayManager(this)
        soundAlertHelper = SoundAlertHelper(this)

        val repo = PreferencesRepositoryImpl(applicationContext.dataStore)
        engine = VisionTimerEngine(currentPrefs)
        engine.onBreakCompleted = {
            soundAlertHelper.playBreakEnded(currentPrefs.soundEnabled, currentPrefs.vibrationEnabled)
        }

        screenReceiver = ScreenStateReceiver(
            onScreenOn = {
                engine.onScreenOn()
                if (engine.isSessionActive) {
                    startTicker()
                }
                if (engine.state.value is TimerStatus.RestBreak) {
                    handleStateChange(engine.state.value)
                }
            },
            onScreenOff = {
                engine.onScreenOff()
                overlayManager.dismissOverlay()
            }
        )
        screenReceiver.register(this)

        serviceScope.launch {
            repo.preferencesFlow.collect { prefs ->
                currentPrefs = prefs
                engine.updatePreferences(prefs)
            }
        }

        serviceScope.launch {
            engine.state.collect { status ->
                VisionTimerStateHolder.setState(status)
                handleStateChange(status)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayManager.onConfigurationChanged(newConfig)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == null) {
            // System restarted a sticky service with a null intent.
            // Restore last known state if session was active, otherwise stop.
            val current = VisionTimerStateHolder.state.value
            if (current is TimerStatus.Idle) {
                stopSelf()
                return Service.START_NOT_STICKY
            }
            // Session was active: re-establish foreground notification and ticker.
            startForegroundServiceWithNotification()
            if (current is TimerStatus.Working && !current.isScreenPaused) {
                startTicker()
            }
            return Service.START_STICKY
        }
        when (intent.action) {
            ACTION_START -> startTimer()
            ACTION_PAUSE -> engine.pause()
            ACTION_RESUME -> engine.resume()
            ACTION_STOP -> stopTimer()
            ACTION_SNOOZE -> engine.snooze()
            ACTION_TRIGGER_BREAK -> engine.triggerBreak()
            ACTION_COMPLETE_BREAK -> engine.completeBreak()
        }
        return Service.START_STICKY
    }

    private fun startTimer() {
        engine.start()
        startForegroundServiceWithNotification()
        startTicker()
    }

    private fun stopTimer() {
        tickerJob?.cancel()
        tickerJob = null
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

    private fun timerNotificationKey(status: TimerStatus): String {
        return when (status) {
            is TimerStatus.Working -> "W:${status.remainingSeconds}:${status.isScreenPaused}"
            is TimerStatus.RestBreak -> "R:${status.remainingSeconds}"
            is TimerStatus.Snoozed -> "S:${status.remainingSeconds}"
            is TimerStatus.Idle -> "I"
        }
    }

    private fun handleStateChange(status: TimerStatus) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Only rebuild and post the timer notification when its content changes.
        val key = timerNotificationKey(status)
        if (key != lastTimerNotificationKey) {
            lastTimerNotificationKey = key
            val notification = notificationHelper.buildTimerNotification(status)
            notificationManager.notify(NotificationHelper.NOTIFICATION_ID_TIMER, notification)
        }

        when (status) {
            is TimerStatus.RestBreak -> {
                if (previousStatus !is TimerStatus.RestBreak) {
                    soundAlertHelper.playBreakStarted(currentPrefs.soundEnabled, currentPrefs.vibrationEnabled)
                }

                if (currentPrefs.enforcementMode == EnforcementMode.BLOCKER_OVERLAY && overlayManager.canDrawOverlays()) {
                    overlayManager.showBreakOverlay(
                        remainingSeconds = status.remainingSeconds,
                        totalSeconds = currentPrefs.restDurationSeconds,
                        strictMode = currentPrefs.strictMode,
                        appTheme = currentPrefs.appTheme,
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
                if (!status.isScreenPaused && tickerJob?.isActive != true) {
                    startTicker()
                }
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

        previousStatus = status

        // Throttle glance widget updates: at most once every 5 seconds.
        val now = SystemClock.uptimeMillis()
        if (now - lastWidgetUpdateMs >= WIDGET_UPDATE_INTERVAL_MS) {
            lastWidgetUpdateMs = now
            serviceScope.launch {
                try {
                    VisionGlanceWidget().updateAll(this@VisionTimerService)
                } catch (e: Exception) {
                    // Ignore if no active glance widget attached
                }
            }
        }
    }

    override fun onDestroy() {
        screenReceiver.unregister(this)
        overlayManager.dismissOverlay()
        soundAlertHelper.release()
        serviceScope.cancel()
        VisionTimerStateHolder.setState(TimerStatus.Idle)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "in.rahulja.vision202020.action.START"
        const val ACTION_PAUSE = "in.rahulja.vision202020.action.PAUSE"
        const val ACTION_RESUME = "in.rahulja.vision202020.action.RESUME"
        const val ACTION_STOP = "in.rahulja.vision202020.action.STOP"
        const val ACTION_SNOOZE = "in.rahulja.vision202020.action.SNOOZE"
        const val ACTION_TRIGGER_BREAK = "in.rahulja.vision202020.action.TRIGGER_BREAK"
        const val ACTION_COMPLETE_BREAK = "in.rahulja.vision202020.action.COMPLETE_BREAK"

        const val WIDGET_UPDATE_INTERVAL_MS = 5000L

        val timerStatus: StateFlow<TimerStatus> = VisionTimerStateHolder.state

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
            when (VisionTimerStateHolder.state.value) {
                is TimerStatus.Idle -> start(context)
                else -> stop(context)
            }
        }
    }
}