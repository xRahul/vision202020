package com.vision202020.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: PreferencesRepository

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tmpFolder.root, "test_preferences.preferences_pb") }
        )
        repository = PreferencesRepositoryImpl(dataStore)
    }

    @Test
    fun testDefaultPreferencesValues() = runTest(testDispatcher) {
        val prefs = repository.preferencesFlow.first()
        assertEquals(20, prefs.workDurationMinutes)
        assertEquals(20, prefs.restDurationSeconds)
        assertEquals(5, prefs.naturalBreakMinutes)
        assertEquals(TimerProfile.MOBILE_SCREEN_AWARE, prefs.timerProfile)
        assertEquals(EnforcementMode.BLOCKER_OVERLAY, prefs.enforcementMode)
        assertEquals(false, prefs.strictMode)
        assertEquals(true, prefs.soundEnabled)
        assertEquals(true, prefs.vibrationEnabled)
    }

    @Test
    fun testUpdatePreferences() = runTest(testDispatcher) {
        repository.updateWorkDuration(25)
        repository.updateRestDuration(30)
        repository.updateNaturalBreakMinutes(10)
        repository.updateTimerProfile(TimerProfile.LAPTOP_DESK)
        repository.updateEnforcementMode(EnforcementMode.NOTIFICATION_ONLY)
        repository.updateStrictMode(true)

        val updated = repository.preferencesFlow.first()
        assertEquals(25, updated.workDurationMinutes)
        assertEquals(30, updated.restDurationSeconds)
        assertEquals(10, updated.naturalBreakMinutes)
        assertEquals(TimerProfile.LAPTOP_DESK, updated.timerProfile)
        assertEquals(EnforcementMode.NOTIFICATION_ONLY, updated.enforcementMode)
        assertTrue(updated.strictMode)
    }
}
