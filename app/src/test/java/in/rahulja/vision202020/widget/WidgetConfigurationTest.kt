package `in`.rahulja.vision202020.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.ui.theme.VisionGlanceColorProviders
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetConfigurationTest {

    @Test
    fun testWidgetReceiverAndProviderInfo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val receiverComponent = ComponentName(context, VisionWidgetReceiver::class.java)

        val receiverInfo = pm.getReceiverInfo(receiverComponent, PackageManager.GET_META_DATA)
        assertNotNull("VisionWidgetReceiver must be declared in manifest", receiverInfo)

        val metaData = receiverInfo.metaData
        assertNotNull("Receiver must have meta-data", metaData)
        val xmlResId = metaData.getInt(AppWidgetManager.META_DATA_APPWIDGET_PROVIDER)
        assertNotEquals("Provider meta-data resource must be present", 0, xmlResId)

        val initialLayoutResId = context.resources.getXml(xmlResId).use { parser ->
            var layoutId = 0
            while (parser.next() != android.content.res.XmlResourceParser.END_DOCUMENT) {
                if (parser.eventType == android.content.res.XmlResourceParser.START_TAG && parser.name == "appwidget-provider") {
                    layoutId = parser.getAttributeResourceValue(
                        "http://schemas.android.com/apk/res/android",
                        "initialLayout",
                        0
                    )
                    break
                }
            }
            layoutId
        }

        assertNotEquals("initialLayout must be defined to prevent launcher crash", 0, initialLayoutResId)

        // Verify the initial layout can actually be inflated
        val view = LayoutInflater.from(context).inflate(initialLayoutResId, null)
        assertNotNull("Initial loading layout must inflate successfully", view)
    }

    @Test
    fun testWorkManagerProguardRulesRetained() {
        val rulesFile = java.io.File("proguard-rules.pro").takeIf { it.exists() }
            ?: java.io.File("app/proguard-rules.pro")
        assertTrue("proguard-rules.pro must exist", rulesFile.exists())
        val content = rulesFile.readText()
        assertTrue(
            "Must keep androidx.work.InputMerger constructor to prevent R8 full-mode stripping",
            content.contains("extends androidx.work.InputMerger") && content.contains("<init>();")
        )
        assertTrue(
            "Must keep androidx.work.ListenableWorker constructor",
            content.contains("extends androidx.work.ListenableWorker")
        )
    }

    @Test
    fun widgetUsesResponsiveSizes() {
        val mode = VisionGlanceWidget().sizeMode
        assertTrue("Widget must be responsive", mode is androidx.glance.appwidget.SizeMode.Responsive)
    }

    @Test
    fun testVisionGlanceColorProvidersConfigured() {
        assertNotNull(VisionGlanceColorProviders)
    }
}
