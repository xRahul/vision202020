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

        SessionStore(context.sessionDataStore)
            .save(SessionSnapshot(TimerStatus.Idle, isSessionActive = false))

        // Path excluded by res/xml/backup_rules.xml and data_extraction_rules.xml.
        assertTrue(File(context.filesDir, "datastore/vision_session.preferences_pb").exists())
        assertEquals(prefsBefore, context.dataStore.data.first().asMap())
    }
}
