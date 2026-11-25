package `in`.rahulja.vision202020.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.domain.SessionSnapshot
import `in`.rahulja.vision202020.domain.TimerStatus
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionStoreWiringTest {

    @Test
    fun sessionSaveGoesToDedicatedFileNotPreferences() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsBefore = context.dataStore.data.first().asMap()

        val snapshot = SessionSnapshot(TimerStatus.Idle, isSessionActive = false, bootCount = 1, screenOffAtMs = 0L)
        val store = SessionStore(context.sessionDataStore)

        store.save(snapshot)

        assertEquals(snapshot, store.load(currentBootCount = 1, nowElapsedMs = 0L))
        // Path excluded by res/xml/backup_rules.xml and data_extraction_rules.xml. The DataStore
        // singleton is process-wide, so it may live under another test's Robolectric files dir.
        val tmpRoot = generateSequence(context.filesDir) { it.parentFile }.drop(3).first()
        assertTrue(
            tmpRoot.walkTopDown().any { it.invariantSeparatorsPath.endsWith("files/datastore/vision_session.preferences_pb") }
        )
        assertEquals(prefsBefore, context.dataStore.data.first().asMap())
    }
}
