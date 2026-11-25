package `in`.rahulja.vision202020.service

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import `in`.rahulja.vision202020.VisionApplication
import `in`.rahulja.vision202020.data.EnforcementMode
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.SessionStore
import `in`.rahulja.vision202020.data.SessionWriteLane
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

    private var tickerJob: Job? = null

    /** The in-flight (or finished) restore decision; null until [restoreSessionIfNeeded] runs. */
    internal var restoreJob: Job? = null

    /** Test seam: lets a test hold the restore open to prove nothing stops the service meanwhile. */
    internal var beforeRestoreLoad: (suspend () -> Unit)? = null

    private lateinit var notificationHelper: NotificationHelper
    internal lateinit var overlayManager: OverlayManager
        private set
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

    /** The break alert is posted once per break; the system chronometer does the counting. */
    private var breakAlertPosted = false

    /** Toggles waiting for restore to finish; a "nothing to resume" restore must not stop the service under them. */
    private var pendingToggles = 0

    private val applicationScope: CoroutineScope
        get() = (application as VisionApplication).applicationScope

    override fun onCreate() {
        super.onCreate()
        // Channels are created once in VisionApplication.onCreate, which always runs first.
        notificationHelper = NotificationHelper(this)
        overlayManager = OverlayManager(this)
        soundAlertHelper = SoundAlertHelper(this)

        val repo = PreferencesRepositoryImpl(applicationContext.dataStore)
        sessionStore = SessionStore(applicationContext.sessionDataStore)
        engine = VisionTimerEngine(currentPrefs)
        engine.onBreakCompleted = { audible ->
            if (audible) {
                soundAlertHelper.playBreakEnded(currentPrefs.soundEnabled, currentPrefs.vibrationEnabled)
            }
        }

        screenReceiver = ScreenStateReceiver(
            onScreenOn = {
                engine.onScreenOn()
                if (engine.isSessionActive) {
                    startTickerIfRunning()
                }
                handleStateChange(engine.state.value)
            },
            onUserPresent = { onUserPresent() },
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
                        // The initial Idle must not overwrite the saved session before restore has read it.
                        if (restoreJob?.isActive == true && status is TimerStatus.Idle && !engine.isSessionActive) {
                            return@collect
                        }
                        // Build on the main thread; only the write runs on the IO lane.
                        val snapshot = SessionSnapshot(
                            status = status,
                            isSessionActive = engine.isSessionActive,
                            bootCount = currentBootCount(),
                            screenOffAtMs = engine.screenOffAtMs
                        )
                        applicationScope.launch(SessionWriteLane.dispatcher) {
                            runCatchingCancellable { sessionStore.save(snapshot) }
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
        // Commands that act on the restored session set the flag themselves, once restore has finished.
        // START and STOP are explicit and immediate: they supersede whatever restore would load.
        if (action == ACTION_START || action == ACTION_STOP) {
            explicitCommandReceived = true
        }
        sessionStopped = false
        // Every path must post a foreground notification promptly (startForegroundService contract).
        if (!startForegroundServiceWithNotification()) return Service.START_NOT_STICKY

        when (action) {
            // onCreate already started the restore; a late RESTORE on a live service has nothing to add.
            ACTION_RESTORE -> Unit

            ACTION_START -> startTimer()

            ACTION_PAUSE -> afterRestore { engine.pause() }

            ACTION_RESUME -> afterRestore { engine.resume() }

            ACTION_RESET -> afterRestore {
                engine.reset()
                startTickerIfRunning()
            }

            ACTION_STOP -> stopTimer()

            ACTION_SNOOZE -> afterRestore { engine.snooze(currentPrefs.snoozeMinutes * 60) }

            ACTION_TRIGGER_BREAK -> afterRestore { engine.triggerBreak() }

            ACTION_COMPLETE_BREAK -> {
                // Strict mode: only the break screen, after the hold or confirm, may complete a break.
                val fromBreakUi = intent.getBooleanExtra(EXTRA_FROM_BREAK_UI, false)
                if (restoreJob?.isActive == true) {
                    // Prefs and the break itself are only known once restore finishes.
                    pendingToggles++
                    serviceScope.launch {
                        try {
                            restoreJob?.join()
                            explicitCommandReceived = true
                            if (!sessionStopped) completeBreakGated(fromBreakUi)
                        } finally {
                            pendingToggles--
                            stopIfNoSession()
                        }
                    }
                } else {
                    completeBreakGated(fromBreakUi)
                }
            }

            ACTION_TOGGLE -> {
                pendingToggles++
                serviceScope.launch {
                    try {
                        restoreJob?.join()
                        explicitCommandReceived = true
                        // A STOP queued behind this toggle wins; do not start a new session after it.
                        if (!sessionStopped) resolveToggle()
                    } finally {
                        pendingToggles--
                    }
                }
            }
        }
        // A command that cannot produce a live session must not leave a foreground service.
        // Clear the flags first so a command arriving before destruction starts foreground cleanly.
        // RESTORE and TOGGLE are exempt: the engine is still Idle while the restore coroutine reads the store.
        // A COMPLETE_BREAK waiting on restore is exempt too.
        val deferred = action == ACTION_RESTORE || action == ACTION_TOGGLE || pendingToggles > 0
        if (!deferred && stopIfNoSession()) {
            // The command skipped restore, so drop the saved session it would have resumed.
            applicationScope.launch(SessionWriteLane.dispatcher) { runCatchingCancellable { sessionStore.clear() } }
        }
        return Service.START_STICKY
    }

    /** Runs [block] against the restored session: immediately when restore is done, else once it finishes. */
    private fun afterRestore(block: () -> Unit) {
        if (restoreJob?.isActive != true) {
            block()
            return
        }
        // Counts as a waiting command so a "nothing to resume" restore does not stop the service under it.
        pendingToggles++
        serviceScope.launch {
            try {
                restoreJob?.join()
                explicitCommandReceived = true
                if (!sessionStopped) block()
            } finally {
                pendingToggles--
                stopIfNoSession()
            }
        }
    }

    private fun completeBreakGated(fromBreakUi: Boolean) {
        // A stale action must not reset a Working or Snoozed session or play the break-ended alert.
        if (engine.state.value !is TimerStatus.RestBreak) return
        if (blockedByStrictMode() && !fromBreakUi) repostBreakAlert() else engine.completeBreak()
    }

    /** Stops the service when no session is live and no command is waiting. Returns whether it stopped. */
    private fun stopIfNoSession(): Boolean {
        if (pendingToggles > 0 || engine.isSessionActive || engine.state.value !is TimerStatus.Idle) return false
        sessionStopped = true
        foregroundStarted = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return true
    }

    /** Tile/widget toggle, resolved against the restored state. Never destroys an active session by accident. */
    internal fun resolveToggle() {
        when (val status = engine.state.value) {
            is TimerStatus.Idle -> startTimer()
            is TimerStatus.Working -> if (status.isScreenPaused) engine.resume() else engine.pause()
            is TimerStatus.RestBreak -> if (currentPrefs.strictMode) repostBreakAlert() else engine.completeBreak()
            is TimerStatus.Snoozed -> engine.triggerBreak()
        }
    }

    /** Unlocked mid-break: the overlay takes over from the lock-screen notification. */
    internal fun onUserPresent() {
        if (engine.state.value is TimerStatus.RestBreak) handleStateChange(engine.state.value)
    }

    private fun blockedByStrictMode(): Boolean = currentPrefs.strictMode && engine.state.value is TimerStatus.RestBreak

    /** A refused strict-mode skip must leave the user a way to the break screen. */
    private fun repostBreakAlert() {
        val status = engine.state.value as? TimerStatus.RestBreak ?: return
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            NotificationHelper.NOTIFICATION_ID_BREAK,
            notificationHelper.buildBreakAlertNotification(status, currentPrefs)
        )
        breakAlertPosted = true
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
        applicationScope.launch(SessionWriteLane.dispatcher) { runCatchingCancellable { sessionStore.clear() } }
        updateWidget(force = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("InlinedApi") // SPECIAL_USE is masked to a safe value by ServiceCompat below API 34.
    private fun startForegroundServiceWithNotification(): Boolean {
        if (foregroundStarted) return true
        val initialNotification =
            notificationHelper.buildTimerNotification(
                engine.state.value,
                currentPrefs.snoozeMinutes,
                currentPrefs.strictMode
            )
        try {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.NOTIFICATION_ID_TIMER,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException (API 31+) is a subclass: a sticky restart
            // from the background may be refused. Give up quietly instead of crashing.
            Log.w(TAG, "Foreground start denied; stopping", e)
            sessionStopped = true
            stopSelf()
            return false
        }
        foregroundStarted = true
        return true
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
            // A restored session may carry a stale screen-off time whatever its status; clear it now
            // so a later break completion does not think the screen is off.
            if (engine.screenOffAtMs != 0L ||
                (current is TimerStatus.Working && current.pauseReason == PauseReason.SCREEN_OFF)
            ) {
                engine.onScreenOn()
                if (engine.isSessionActive) {
                    startTickerIfRunning()
                }
            }
        }
    }

    private fun restoreSessionIfNeeded() {
        restoreJob = serviceScope.launch {
            beforeRestoreLoad?.invoke()
            val snapshot = runCatchingCancellable { sessionStore.load(currentBootCount()) }.getOrNull()
            if (explicitCommandReceived) return@launch
            if (snapshot == null || !snapshot.isResumable) {
                // A toggle waiting on this restore will start a fresh session; keep the service alive for it.
                if (pendingToggles > 0) return@launch
                // Nothing to resume: drop the foreground notification a RESTORE start posted.
                sessionStopped = true
                foregroundStarted = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }
            engine.restore(snapshot)
            currentPrefs = runCatchingCancellable { PreferencesRepositoryImpl(applicationContext.dataStore).preferencesFlow.first() }
                .getOrDefault(currentPrefs)
            engine.updatePreferences(currentPrefs)
            syncInteractiveState()
            if (!startForegroundServiceWithNotification()) return@launch
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
        val key = timerNotificationKey(status) + currentPrefs.strictMode
        if (key != lastTimerNotificationKey) {
            lastTimerNotificationKey = key
            val notification =
                notificationHelper.buildTimerNotification(status, currentPrefs.snoozeMinutes, currentPrefs.strictMode)
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

                val locked = (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true
                // Locked: an overlay would sit behind the keyguard, so the full-screen notification delivers the break.
                val wantsOverlay = currentPrefs.enforcementMode == EnforcementMode.BLOCKER_OVERLAY && !locked
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
                if (overlayManager.isShowing) {
                    if (breakAlertPosted) {
                        notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_BREAK)
                        breakAlertPosted = false
                    }
                } else if (!breakAlertPosted) {
                    // Locked, no overlay permission, or OEM refused the window: always fall back.
                    val alertNotification =
                        notificationHelper.buildBreakAlertNotification(status, currentPrefs)
                    notificationManager.notify(
                        NotificationHelper.NOTIFICATION_ID_BREAK,
                        alertNotification
                    )
                    breakAlertPosted = true
                }
            }

            is TimerStatus.Working -> {
                breakAlertPosted = false
                overlayManager.dismissOverlay()
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_BREAK)
                if (status.isScreenPaused) {
                    stopTicker()
                } else if (tickerJob?.isActive != true) {
                    startTicker()
                }
            }

            is TimerStatus.Snoozed -> {
                breakAlertPosted = false
                overlayManager.dismissOverlay()
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_BREAK)
                if (tickerJob?.isActive != true) startTicker()
            }

            is TimerStatus.Idle -> {
                breakAlertPosted = false
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
            runCatchingCancellable { VisionGlanceWidget().updateAll(applicationContext) }
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
        const val ACTION_TOGGLE = "in.rahulja.vision202020.action.TOGGLE"

        /** Set by the break screen after the hold or accessibility confirm; strict mode requires it to complete a break. */
        const val EXTRA_FROM_BREAK_UI = "in.rahulja.vision202020.extra.FROM_BREAK_UI"

        private const val TAG = "VisionTimerService"

        const val WIDGET_UPDATE_INTERVAL_MS = 5000L

        val timerStatus: StateFlow<TimerStatus> = VisionTimerStateHolder.state

        fun start(context: Context) = send(context, ACTION_START)

        fun restore(context: Context) = send(context, ACTION_RESTORE)

        fun pause(context: Context) = send(context, ACTION_PAUSE)

        fun resume(context: Context) = send(context, ACTION_RESUME)

        fun reset(context: Context) = send(context, ACTION_RESET)

        fun stop(context: Context) = send(context, ACTION_STOP)

        fun triggerBreak(context: Context) = send(context, ACTION_TRIGGER_BREAK)

        fun completeBreak(context: Context, fromBreakUi: Boolean = false) =
            send(context, ACTION_COMPLETE_BREAK) { putExtra(EXTRA_FROM_BREAK_UI, fromBreakUi) }

        fun snooze(context: Context) = send(context, ACTION_SNOOZE)

        /** QS tile / widget toggle. The service resolves it after restore, so it acts on the restored session. */
        fun toggle(context: Context): Boolean = send(context, ACTION_TOGGLE)

        /** Returns false when Android denies the foreground-service start (e.g. background start restrictions). */
        private fun send(context: Context, action: String, extras: Intent.() -> Unit = {}): Boolean {
            val intent = Intent(context, VisionTimerService::class.java).apply {
                this.action = action
                extras()
            }
            return try {
                // Works from the tile/widget while the app is in the background.
                ContextCompat.startForegroundService(context, intent)
                true
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException (API 31+) is a subclass.
                Log.w(TAG, "Foreground service start denied for $action", e)
                false
            }
        }
    }
}
