package `in`.rahulja.vision202020.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.SessionStore
import `in`.rahulja.vision202020.data.SessionWriteLane
import `in`.rahulja.vision202020.data.currentBootCount
import `in`.rahulja.vision202020.data.dataStore
import `in`.rahulja.vision202020.data.sessionDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        // goAsync() keeps the process alive until the preference read completes.
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val sessionStore = SessionStore(context.applicationContext.sessionDataStore)
                if (action == Intent.ACTION_BOOT_COMPLETED) {
                    // elapsedRealtime restarted: any saved session is from the previous boot. Clear it
                    // on the shared write lane so no older save can land after the clear.
                    withContext(SessionWriteLane.dispatcher) { runCatching { sessionStore.clear() } }
                }
                val repository = PreferencesRepositoryImpl(context.applicationContext.dataStore)
                val prefs = repository.preferencesFlow.first()
                if (action == Intent.ACTION_BOOT_COMPLETED) {
                    if (prefs.startOnBoot) {
                        VisionTimerService.start(context)
                    }
                } else if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                    val snapshot = runCatching { sessionStore.load(context.currentBootCount()) }.getOrNull()
                    if (snapshot?.isResumable == true) {
                        VisionTimerService.restore(context)
                    } else if (prefs.startOnBoot) {
                        VisionTimerService.start(context)
                    }
                }
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }
}
