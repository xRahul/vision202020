package `in`.rahulja.vision202020.crash

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes the last uncaught exception to app-private storage.
 * No network access, no upload, no third-party SDK.
 */
object CrashLogWriter {

    const val FILE_NAME = "last-crash.txt"

    @Volatile
    private var appContext: Context? = null

    fun file(context: Context): File = File(File(context.filesDir, "crash-logs"), FILE_NAME)

    fun install(context: Context) {
        appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun write(thread: Thread, throwable: Throwable) {
        val context = appContext ?: return
        val target = file(context)
        target.parentFile?.mkdirs()
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
            .format(Date())
        val content = buildString {
            appendLine("time=$timestamp")
            appendLine("thread=${thread.name}")
            appendLine(Log.getStackTraceString(throwable))
        }
        // Write to a temp file then rename so a failure mid-write keeps the previous log.
        val temp = File(target.parentFile, "$FILE_NAME.tmp")
        try {
            temp.writeText(content)
            if (!temp.renameTo(target)) error("rename failed")
        } catch (t: Throwable) {
            temp.delete()
            throw t
        }
    }
}
