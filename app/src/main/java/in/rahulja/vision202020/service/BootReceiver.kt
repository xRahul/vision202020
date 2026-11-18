package `in`.rahulja.vision202020.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.rahulja.vision202020.data.PreferencesRepositoryImpl
import `in`.rahulja.vision202020.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // goAsync() keeps the process alive until the preference read completes.
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val repository = PreferencesRepositoryImpl(context.applicationContext.dataStore)
                val prefs = repository.preferencesFlow.first()
                if (prefs.startOnBoot) {
                    VisionTimerService.start(context)
                }
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }
}