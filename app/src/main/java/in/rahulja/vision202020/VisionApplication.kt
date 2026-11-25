package `in`.rahulja.vision202020

import android.app.Application
import `in`.rahulja.vision202020.crash.CrashLogWriter
import `in`.rahulja.vision202020.data.SessionStore
import `in`.rahulja.vision202020.data.dataStore
import `in`.rahulja.vision202020.data.sessionDataStore
import `in`.rahulja.vision202020.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VisionApplication : Application() {

    /**
     * Process-lifetime scope. Widget refreshes and session persistence must finish even
     * when the foreground service is stopping or already destroyed.
     */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        CrashLogWriter.install(this)
        NotificationHelper(this).createChannels()
        applicationScope.launch {
            runCatching { SessionStore(sessionDataStore).clearLegacy(dataStore) }
        }
    }
}
