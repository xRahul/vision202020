package `in`.rahulja.vision202020.data

import android.content.Context
import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.rahulja.vision202020.domain.SessionSnapshot
import `in`.rahulja.vision202020.domain.SessionSnapshotCodec
import `in`.rahulja.vision202020.domain.TimerStatus
import kotlinx.coroutines.flow.first

/** Separate file so session saves never re-emit user preferences (and vice versa). */
val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "vision_session")

/**
 * Persists the active session so a process death or OS service restart resumes the
 * same countdown instead of silently dropping it.
 */
class SessionStore(private val dataStore: DataStore<Preferences>) {

    /**
     * Returns the saved session only if it belongs to the current boot. When the boot count is
     * unavailable (-1) a deadline far ahead of elapsedRealtime means the clock restarted.
     */
    suspend fun load(currentBootCount: Int, nowElapsedMs: Long = SystemClock.elapsedRealtime()): SessionSnapshot? {
        val raw = dataStore.data.first()[Keys.SESSION]
        val snapshot = SessionSnapshotCodec.decode(raw) ?: return null
        if (snapshot.bootCount != currentBootCount) return null
        if ((currentBootCount == -1 || snapshot.bootCount == -1) &&
            snapshot.farFuture(nowElapsedMs)
        ) {
            return null
        }
        return snapshot
    }

    private fun SessionSnapshot.farFuture(nowElapsedMs: Long): Boolean {
        val limit = nowElapsedMs + MAX_AHEAD_MS
        return when (val s = status) {
            is TimerStatus.Working -> !s.isScreenPaused && s.deadlineTimestampMs > limit
            is TimerStatus.RestBreak -> s.breakStartedTimestampMs > limit
            is TimerStatus.Snoozed -> s.deadlineTimestampMs > limit
            is TimerStatus.Idle -> false
        }
    }

    suspend fun save(snapshot: SessionSnapshot) {
        dataStore.edit { preferences ->
            preferences[Keys.SESSION] = SessionSnapshotCodec.encode(snapshot)
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.SESSION)
        }
    }

    /** Removes the session key that used to live in the shared preferences file. */
    suspend fun clearLegacy(prefsStore: DataStore<Preferences>) {
        prefsStore.edit { preferences ->
            preferences.remove(Keys.SESSION)
        }
    }

    private companion object {
        const val MAX_AHEAD_MS = 3_600_000L
    }

    private object Keys {
        val SESSION = stringPreferencesKey("active_session_snapshot")
    }
}
