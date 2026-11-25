package `in`.rahulja.vision202020.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: PreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_preferences.preferences_pb") }
        )
        repository = PreferencesRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testScope.cancel()
    }

    @Test
    fun testDefaultPreferencesValues() = runTest(testDispatcher) {
        val prefs = repository.preferencesFlow.first()
        assertEquals(20, prefs.workDurationMinutes)
        assertEquals(20, prefs.restDurationSeconds)
        assertEquals(5, prefs.naturalBreakMinutes)
        assertEquals(5, prefs.snoozeMinutes)
        assertEquals(EnforcementMode.BLOCKER_OVERLAY, prefs.enforcementMode)
        assertEquals(false, prefs.strictMode)
        assertEquals(true, prefs.soundEnabled)
        assertEquals(true, prefs.vibrationEnabled)
        assertEquals(AppThemeMode.OLED_BLACK, prefs.appTheme)
    }

    @Test
    fun testUpdatePreferences() = runTest(testDispatcher) {
        repository.updateWorkDuration(25)
        repository.updateRestDuration(30)
        repository.updateNaturalBreakMinutes(10)
        repository.updateSnoozeMinutes(10)
        repository.updateEnforcementMode(EnforcementMode.NOTIFICATION_ONLY)
        repository.updateStrictMode(true)
        repository.updateAppTheme(AppThemeMode.LIGHT)

        val updated = repository.preferencesFlow.first()
        assertEquals(25, updated.workDurationMinutes)
        assertEquals(30, updated.restDurationSeconds)
        assertEquals(10, updated.naturalBreakMinutes)
        assertEquals(10, updated.snoozeMinutes)
        assertEquals(EnforcementMode.NOTIFICATION_ONLY, updated.enforcementMode)
        assertTrue(updated.strictMode)
        assertEquals(AppThemeMode.LIGHT, updated.appTheme)
    }

    @Test
    fun outOfRangeValuesAreClamped() = runTest(testDispatcher) {
        repository.updateWorkDuration(0)
        repository.updateRestDuration(99999)
        repository.updateNaturalBreakMinutes(-4)
        repository.updateSnoozeMinutes(1000)
        val written = repository.preferencesFlow.first()
        assertEquals(PreferenceLimits.WORK_MINUTES.first, written.workDurationMinutes)
        assertEquals(PreferenceLimits.REST_SECONDS.last, written.restDurationSeconds)
        assertEquals(PreferenceLimits.NATURAL_BREAK_MINUTES.first, written.naturalBreakMinutes)
        assertEquals(PreferenceLimits.SNOOZE_MINUTES.last, written.snoozeMinutes)
    }

    @Test
    fun outOfRangeStoredValuesAreClampedOnRead() = runTest(testDispatcher) {
        dataStore.edit {
            it[intPreferencesKey("work_duration_minutes")] = 0
            it[intPreferencesKey("rest_duration_seconds")] = 99999
        }
        val prefs = repository.preferencesFlow.first()
        assertEquals(PreferenceLimits.WORK_MINUTES.first, prefs.workDurationMinutes)
        assertEquals(PreferenceLimits.REST_SECONDS.last, prefs.restDurationSeconds)
    }
}
