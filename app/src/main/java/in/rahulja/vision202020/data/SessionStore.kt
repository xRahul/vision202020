package `in`.rahulja.vision202020.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.rahulja.vision202020.domain.SessionSnapshot
import `in`.rahulja.vision202020.domain.SessionSnapshotCodec
import kotlinx.coroutines.flow.first

/** Separate file so session saves never re-emit user preferences (and vice versa). */
val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "vision_session")

/**
 * Persists the active session so a process death or OS service restart resumes the
 * same countdown instead of silently dropping it.
 */
class SessionStore(private val dataStore: DataStore<Preferences>) {

    suspend fun load(): SessionSnapshot? {
        val raw = dataStore.data.first()[Keys.SESSION]
        return SessionSnapshotCodec.decode(raw)
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

    private object Keys {
        val SESSION = stringPreferencesKey("active_session_snapshot")
    }
}
