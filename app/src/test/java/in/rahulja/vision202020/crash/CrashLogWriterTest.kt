package `in`.rahulja.vision202020.crash

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class CrashLogWriterTest {

    @Test
    fun writesStackTraceToAppPrivateStorage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        CrashLogWriter.install(context)

        CrashLogWriter.write(
            Thread.currentThread(),
            IllegalStateException("test crash marker")
        )

        val log = CrashLogWriter.file(context)
        assertTrue(log.exists())
        assertTrue(log.readText().contains("test crash marker"))
    }
}
