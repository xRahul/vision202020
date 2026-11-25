package `in`.rahulja.vision202020

import android.appwidget.AppWidgetManager
import androidx.test.platform.app.InstrumentationRegistry
import `in`.rahulja.vision202020.widget.VisionWidgetReceiver
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSmokeTest {

    @Test
    fun providerIsRegistered() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val providers = AppWidgetManager.getInstance(context).installedProviders
            .filter { it.provider.packageName == context.packageName }
            .map { it.provider.className }
        assertTrue("providers=$providers", VisionWidgetReceiver::class.java.name in providers)
    }
}
