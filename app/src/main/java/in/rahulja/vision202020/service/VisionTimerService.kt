package `in`.rahulja.vision202020.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import `in`.rahulja.vision202020.data.EnforcementMode
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.UserPreferences
import `in`.rahulja.vision202020.data.dataStore
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.domain.VisionTimerEngine
import `in`.rahulja.vision202020.overlay.OverlayManager
import androidx.glance.appwidget.updateAll
import `in`.rahulja.vision202020.widget.VisionGlanceWidget
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
    private lateinit var soundAlertHelper: SoundAlertHelper
    private lateinit var engine: VisionTimerEngine
    private lateinit var screenReceiver: ScreenStateReceiver

    private var currentPrefs = UserPreferences()
    private var previousStatus: TimerStatus? = null

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
                tickerJob?.cancel()
                tickerJob = null
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

    private fun handleStateChange(status: TimerStatus) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = notificationHelper.buildTimerNotification(status)
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID_TIMER, notification)

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

        serviceScope.launch {
            try {
                VisionGlanceWidget().updateAll(this@VisionTimerService)
            } catch (e: Exception) {
                // Ignore if no active glance widget attached
            }
        }
    }

    override fun onDestroy() {
        screenReceiver.unregister(this)
        overlayManager.dismissOverlay()
        soundAlertHelper.release()
        serviceScope.cancel()
        _timerStatus.value = TimerStatus.Idle
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
