package `in`.rahulja.vision202020.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import `in`.rahulja.vision202020.domain.PauseReason
import `in`.rahulja.vision202020.domain.SessionSnapshot
import `in`.rahulja.vision202020.domain.TimerStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SessionStoreTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: SessionStore

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("session.preferences_pb") }
        )
        store = SessionStore(dataStore)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `load returns null before anything is saved`() = runTest(testDispatcher) {
        assertNull(store.load())
    }

    @Test
    fun `saved running session round-trips`() = runTest(testDispatcher) {
        val snapshot = SessionSnapshot(
            TimerStatus.Working(
                remainingSeconds = 900,
                elapsedSeconds = 300,
                totalSeconds = 1200,
                pauseReason = PauseReason.NONE,
                deadlineTimestampMs = 999_000L
            ),
            isSessionActive = true
        )

        store.save(snapshot)

        assertEquals(snapshot, store.load())
        assertTrue(store.load()!!.isResumable)
    }

    @Test
    fun `clear removes the saved session`() = runTest(testDispatcher) {
        store.save(SessionSnapshot(TimerStatus.Idle, isSessionActive = false))
        store.clear()

        assertNull(store.load())
    }

    @Test
    fun savesToDedicatedFile() = runTest(testDispatcher) {
        val prefsStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("prefs.preferences_pb") }
        )
        val emissions = mutableListOf<Preferences>()
        val collector = launch { prefsStore.data.collect { emissions.add(it) } }
        advanceUntilIdle()
        val before = emissions.size

        store.save(SessionSnapshot(TimerStatus.Idle, isSessionActive = false))
        advanceUntilIdle()

        assertEquals(1, before)
        assertEquals(before, emissions.size)
        collector.cancel()
    }

    @Test
    fun clearLegacyRemovesOldKey() = runTest(testDispatcher) {
        val legacyKey = stringPreferencesKey("active_session_snapshot")
        val otherKey = stringPreferencesKey("other")
        val prefsStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("legacy.preferences_pb") }
        )
        prefsStore.edit {
            it[legacyKey] = "stale"
            it[otherKey] = "keep"
        }

        store.clearLegacy(prefsStore)

        val result = prefsStore.data.first()
        assertNull(result[legacyKey])
        assertEquals("keep", result[otherKey])
    }

    @Test
    fun `idle or inactive snapshots are not resumable`() {
        assertFalse(SessionSnapshot(TimerStatus.Idle, true).isResumable)
        assertFalse(
            SessionSnapshot(
                TimerStatus.Working(remainingSeconds = 10, elapsedSeconds = 0, totalSeconds = 10),
                isSessionActive = false
            ).isResumable
        )
    }
}
