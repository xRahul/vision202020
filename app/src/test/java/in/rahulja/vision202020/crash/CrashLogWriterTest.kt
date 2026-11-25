package `in`.rahulja.vision202020.crash

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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

    @Test
    fun writeIsAtomic() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        CrashLogWriter.install(context)
        CrashLogWriter.write(Thread.currentThread(), IllegalStateException("old marker"))
        val log = CrashLogWriter.file(context)

        // Block the temp file so the write fails mid-way; the old log must survive.
        val tmp = File(log.parentFile, "${CrashLogWriter.FILE_NAME}.tmp")
        assertTrue(tmp.mkdirs())
        runCatching { CrashLogWriter.write(Thread.currentThread(), IllegalStateException("new marker")) }
        assertTrue(log.readText().contains("old marker"))
        assertFalse(log.readText().contains("new marker"))

        // Once unblocked, the new log replaces the old one and no temp file remains.
        tmp.delete()
        CrashLogWriter.write(Thread.currentThread(), IllegalStateException("new marker"))
        assertTrue(log.readText().contains("new marker"))
        assertFalse(tmp.exists())
        assertEquals(1, log.parentFile!!.listFiles()!!.size)
    }
}
