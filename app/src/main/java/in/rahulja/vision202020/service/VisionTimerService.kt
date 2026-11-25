package `in`.rahulja.vision202020.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import `in`.rahulja.vision202020.VisionApplication
import `in`.rahulja.vision202020.data.EnforcementMode
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.SessionStore
import `in`.rahulja.vision202020.data.UserPreferences
import `in`.rahulja.vision202020.data.currentBootCount
import `in`.rahulja.vision202020.data.dataStore
import `in`.rahulja.vision202020.data.sessionDataStore
import `in`.rahulja.vision202020.domain.PauseReason
import `in`.rahulja.vision202020.domain.SessionSnapshot
import `in`.rahulja.vision202020.domain.TimerStatus
import `in`.rahulja.vision202020.domain.VisionTimerEngine
import `in`.rahulja.vision202020.domain.VisionTimerStateHolder
import `in`.rahulja.vision202020.domain.sessionKey
import `in`.rahulja.vision202020.overlay.OverlayManager
import `in`.rahulja.vision202020.widget.VisionGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VisionTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // A single sequential lane keeps snapshot writes in submission order, so a fast
    // break->complete transition cannot persist the older state last.
    private val sessionWriteDispatcher = Dispatchers.IO.limitedParallelism(1)
    private var tickerJob: Job? = null

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var overlayManager: OverlayManager
    private lateinit var soundAlertHelper: SoundAlertHelper
    private lateinit var engine: VisionTimerEngine
    private lateinit var screenReceiver: ScreenStateReceiver
    private lateinit var sessionStore: SessionStore

    private var currentPrefs = UserPreferences()
    private var previousStatus: TimerStatus? = null
    private var lastWidgetUpdateMs: Long = 0L
    private var lastTimerNotificationKey: String? = null
    private var foregroundStarted = false
    private var explicitCommandReceived = false
    private var sessionStopped = false

    private val applicationScope: CoroutineScope
        get() = (application as VisionApplication).applicationScope

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        notificationHelper.createChannels()
        overlayManager = OverlayManager(this)
        soundAlertHelper = SoundAlertHelper(this)

        val repo = PreferencesRepositoryImpl(applicationContext.dataStore)
        sessionStore = SessionStore(applicationContext.sessionDataStore)
        engine = VisionTimerEngine(currentPrefs)
        engine.onBreakCompleted = {
            soundAlertHelper.playBreakEnded(currentPrefs.soundEnabled, currentPrefs.vibrationEnabled)
        }

        screenReceiver = ScreenStateReceiver(
            onScreenOn = {
                engine.onScreenOn()
                if (engine.isSessionActive) {
                    startTickerIfRunning()
                }
                handleStateChange(engine.state.value)
            },
            onScreenOff = {
                engine.onScreenOff()
                overlayManager.dismissOverlay()
                // Zero wakeups while the screen is off: the engine keeps the deadline.
                stopTicker()
            }
        )
        screenReceiver.register(this)
        syncInteractiveState()

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

        // Persist only real transitions, not every one-second tick. Screen-off time is part of
        // the key so screen-off/on transitions persist too.
        engine.state
            .map { status -> status to (engine.screenOffAtMs != 0L) }
            .distinctUntilChanged { old, new ->
                old.first.sessionKey() == new.first.sessionKey() && old.second == new.second
            }
            .let { flow ->
                serviceScope.launch {
                    flow.collect { (status, _) ->
                        // Build on the main thread; only the write runs on the IO lane.
                        val snapshot = SessionSnapshot(
                            status = status,
                            isSessionActive = engine.isSessionActive,
                            bootCount = currentBootCount(),
                            screenOffAtMs = engine.screenOffAtMs
                        )
                        applicationScope.launch(sessionWriteDispatcher) {
                            runCatching { sessionStore.save(snapshot) }
                        }
                    }
                }
            }

        restoreSessionIfNeeded()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayManager.onConfigurationChanged(newConfig)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == null) {
            // OS restarted a sticky service. restoreSessionIfNeeded() decides to resume or stop.
            return Service.START_STICKY
        }
        if (action != ACTION_RESTORE) {
            explicitCommandReceived = true
        }
        sessionStopped = false
        // Every path must post a foreground notification promptly (startForegroundService contract).
        startForegroundServiceWithNotification()

        when (action) {
            ACTION_RESTORE -> restoreSessionIfNeeded()

            ACTION_START -> startTimer()

            ACTION_PAUSE -> engine.pause()

            ACTION_RESUME -> engine.resume()

            ACTION_RESET -> {
                engine.reset()
                startTickerIfRunning()
            }

            ACTION_STOP -> stopTimer()

            ACTION_SNOOZE -> engine.snooze(currentPrefs.snoozeMinutes * 60)

            ACTION_TRIGGER_BREAK -> engine.triggerBreak()

            ACTION_COMPLETE_BREAK -> engine.completeBreak()
        }
        // A command that cannot produce a live session must not leave a foreground service.
        // Clear the flags first so a command arriving before destruction starts foreground cleanly.
        if (!engine.isSessionActive && engine.state.value is TimerStatus.Idle) {
            sessionStopped = true
            foregroundStarted = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return Service.START_STICKY
    }

    private fun startTimer() {
        sessionStopped = false
        engine.start()
        syncInteractiveState()
        startTickerIfRunning()
        handleStateChange(engine.state.value)
    }

    private fun stopTimer() {
        sessionStopped = true
        stopTicker()
        engine.stop()
        // Publish Idle synchronously so widget/UI render the stopped state even though
        // the state collector is cancelled in onDestroy.
        VisionTimerStateHolder.setState(TimerStatus.Idle)
        overlayManager.dismissOverlay()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_TIMER)
        notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_BREAK)
        foregroundStarted = false
        applicationScope.launch(sessionWriteDispatcher) { runCatching { sessionStore.clear() } }
        updateWidget(force = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("InlinedApi") // SPECIAL_USE is masked to a safe value by ServiceCompat below API 34.
    private fun startForegroundServiceWithNotification() {
        if (foregroundStarted) return
        val initialNotification =
            notificationHelper.buildTimerNotification(engine.state.value, currentPrefs.snoozeMinutes)
        ServiceCompat.startForeground(
            this,
            NotificationHelper.NOTIFICATION_ID_TIMER,
            initialNotification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        foregroundStarted = true
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                engine.tickSecond()
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    /** Start the ticker only when a live countdown needs it. */
    private fun startTickerIfRunning() {
        when (val status = engine.state.value) {
            is TimerStatus.Working -> if (!status.isScreenPaused) startTicker() else stopTicker()
            is TimerStatus.RestBreak, is TimerStatus.Snoozed -> startTicker()
            is TimerStatus.Idle -> stopTicker()
        }
    }

    private fun syncInteractiveState() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isInteractive = powerManager?.isInteractive ?: true
        if (!isInteractive) {
            engine.onScreenOff()
            overlayManager.dismissOverlay()
            stopTicker()
        } else {
            val current = engine.state.value
            if (current is TimerStatus.Working && current.pauseReason == PauseReason.SCREEN_OFF) {
                engine.onScreenOn()
                if (engine.isSessionActive) {
                    startTickerIfRunning()
                }
            }
        }
    }

    private fun restoreSessionIfNeeded() {
        serviceScope.launch {
            val snapshot = runCatching { sessionStore.load(currentBootCount()) }.getOrNull()
            if (explicitCommandReceived) return@launch
            if (snapshot == null || !snapshot.isResumable) {
                stopSelf()
                return@launch
            }
            engine.restore(snapshot)
            currentPrefs = runCatching { PreferencesRepositoryImpl(applicationContext.dataStore).preferencesFlow.first() }
                .getOrDefault(currentPrefs)
            engine.updatePreferences(currentPrefs)
            syncInteractiveState()
            startForegroundServiceWithNotification()
            startTickerIfRunning()
            handleStateChange(engine.state.value)
        }
    }

    private fun timerNotificationKey(status: TimerStatus): String = when (status) {
        is TimerStatus.Working -> "W:${status.remainingSeconds}:${status.pauseReason}"
        is TimerStatus.RestBreak -> "R:${status.remainingSeconds}"
        is TimerStatus.Snoozed -> "S:${status.remainingSeconds}"
        is TimerStatus.Idle -> "I"
    }

    private fun handleStateChange(status: TimerStatus) {
        // After stopTimer() the queued Idle emission must not resurrect a notification.
        if (sessionStopped) return
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Only rebuild the timer notification when its content changes.
        val key = timerNotificationKey(status)
        if (key != lastTimerNotificationKey) {
            lastTimerNotificationKey = key
            val notification =
                notificationHelper.buildTimerNotification(status, currentPrefs.snoozeMinutes)
            notificationManager.notify(NotificationHelper.NOTIFICATION_ID_TIMER, notification)
        }

        when (status) {
            is TimerStatus.RestBreak -> {
                if (previousStatus !is TimerStatus.RestBreak) {
                    soundAlertHelper.playBreakStarted(
                        currentPrefs.soundEnabled,
                        currentPrefs.vibrationEnabled
                    )
                }

                val wantsOverlay = currentPrefs.enforcementMode == EnforcementMode.BLOCKER_OVERLAY
                if (wantsOverlay && overlayManager.canDrawOverlays()) {
                    overlayManager.showBreakOverlay(
                        remainingSeconds = status.remainingSeconds,
                        totalSeconds = status.totalSeconds,
                        strictMode = currentPrefs.strictMode,
                        snoozeMinutes = currentPrefs.snoozeMinutes,
                        appTheme = currentPrefs.appTheme,
                        onSkip = { engine.completeBreak() },
                        onSnooze = { engine.snooze(currentPrefs.snoozeMinutes * 60) }
                    )
                }
                if (!overlayManager.isShowing) {
                    // No overlay permission or OEM refused the window: always fall back.
                    val alertNotification =
                        notificationHelper.buildBreakAlertNotification(status, currentPrefs)
                    notificationManager.notify(
                        NotificationHelper.NOTIFICATION_ID_BREAK,
                        alertNotification
                    )
                }
            }

            is TimerStatus.Working -> {
                overlayManager.dismissOverlay()
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_BREAK)
                if (status.isScreenPaused) {
                    stopTicker()
                } else if (tickerJob?.isActive != true) {
                    startTicker()
                }
            }

            is TimerStatus.Snoozed -> {
                overlayManager.dismissOverlay()
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_BREAK)
                if (tickerJob?.isActive != true) startTicker()
            }

            is TimerStatus.Idle -> {
                overlayManager.dismissOverlay()
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_BREAK)
                stopTicker()
            }
        }

        val isStatusClassChanged = previousStatus?.javaClass != status.javaClass
        val isScreenPausedChanged = (previousStatus as? TimerStatus.Working)?.isScreenPaused !=
            (status as? TimerStatus.Working)?.isScreenPaused
        val isForce = status is TimerStatus.Idle || isStatusClassChanged || isScreenPausedChanged

        previousStatus = status

        // Idle or state transition: never throttle or the widget can stay stale forever.
        updateWidget(force = isForce)
    }

    private fun updateWidget(force: Boolean) {
        val now = SystemClock.uptimeMillis()
        if (!force && now - lastWidgetUpdateMs < WIDGET_UPDATE_INTERVAL_MS) return
        lastWidgetUpdateMs = now
        applicationScope.launch {
            runCatching { VisionGlanceWidget().updateAll(applicationContext) }
        }
    }

    override fun onDestroy() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_TIMER)
        notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_BREAK)
        screenReceiver.unregister(this)
        overlayManager.dismissOverlay()
        soundAlertHelper.release()
        stopTicker()
        serviceScope.cancel()
        VisionTimerStateHolder.setState(TimerStatus.Idle)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "in.rahulja.vision202020.action.START"
        const val ACTION_RESTORE = "in.rahulja.vision202020.action.RESTORE"
        const val ACTION_PAUSE = "in.rahulja.vision202020.action.PAUSE"
        const val ACTION_RESUME = "in.rahulja.vision202020.action.RESUME"
        const val ACTION_RESET = "in.rahulja.vision202020.action.RESET"
        const val ACTION_STOP = "in.rahulja.vision202020.action.STOP"
        const val ACTION_SNOOZE = "in.rahulja.vision202020.action.SNOOZE"
        const val ACTION_TRIGGER_BREAK = "in.rahulja.vision202020.action.TRIGGER_BREAK"
        const val ACTION_COMPLETE_BREAK = "in.rahulja.vision202020.action.COMPLETE_BREAK"

        const val WIDGET_UPDATE_INTERVAL_MS = 5000L

        val timerStatus: StateFlow<TimerStatus> = VisionTimerStateHolder.state

        fun start(context: Context) = send(context, ACTION_START)

        fun restore(context: Context) = send(context, ACTION_RESTORE)

        fun pause(context: Context) = send(context, ACTION_PAUSE)

        fun resume(context: Context) = send(context, ACTION_RESUME)

        fun reset(context: Context) = send(context, ACTION_RESET)

        fun stop(context: Context) = send(context, ACTION_STOP)

        fun triggerBreak(context: Context) = send(context, ACTION_TRIGGER_BREAK)

        fun completeBreak(context: Context) = send(context, ACTION_COMPLETE_BREAK)

        fun snooze(context: Context) = send(context, ACTION_SNOOZE)

        /** QS tile / widget toggle: never destroys an active session by accident. */
        fun toggle(context: Context) {
            when (val status = VisionTimerStateHolder.state.value) {
                is TimerStatus.Idle -> start(context)
                is TimerStatus.Working -> if (status.isScreenPaused) resume(context) else pause(context)
                is TimerStatus.RestBreak -> completeBreak(context)
                is TimerStatus.Snoozed -> triggerBreak(context)
            }
        }

        private fun send(context: Context, action: String) {
            val intent = Intent(context, VisionTimerService::class.java).apply { this.action = action }
            // Works from the tile/widget while the app is in the background.
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
