package `in`.rahulja.vision202020

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.crash.CrashLogWriter
import `in`.rahulja.vision202020.service.BootReceiver
import `in`.rahulja.vision202020.ui.breakui.BreakActivity
import `in`.rahulja.vision202020.ui.main.MainActivity
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.w3c.dom.Document
import org.w3c.dom.Element

@RunWith(RobolectricTestRunner::class)
class ManifestSanityTest {

    @Test
    fun testPackageConfiguration() {
        val appClass = VisionApplication::class.java
        assertNotNull(appClass)
    }

    @Test
    fun testActivityConfigChanges() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val mainActivityInfo = pm.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            PackageManager.GET_META_DATA
        )
        val breakActivityInfo = pm.getActivityInfo(
            ComponentName(context, BreakActivity::class.java),
            PackageManager.GET_META_DATA
        )
        val expectedConfigChanges = ActivityInfo.CONFIG_ORIENTATION or
            ActivityInfo.CONFIG_SCREEN_SIZE or
            ActivityInfo.CONFIG_SCREEN_LAYOUT or
            ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE

        assertEquals(
            "MainActivity must handle orientation and screen size config changes",
            expectedConfigChanges,
            mainActivityInfo.configChanges and expectedConfigChanges
        )
        assertEquals(
            "BreakActivity must handle orientation and screen size config changes",
            expectedConfigChanges,
            breakActivityInfo.configChanges and expectedConfigChanges
        )
    }

    @Test
    fun backupRulesExcludeSessionAndCrashLog() {
        val sessionPath = "datastore/vision_session.preferences_pb"
        val crashPath = "crash-logs/${CrashLogWriter.FILE_NAME}"

        val extraction = parseXml("src/main/res/xml/data_extraction_rules.xml")
        val fullBackup = parseXml("src/main/res/xml/backup_rules.xml")

        val cloud = extraction.getElementsByTagName("cloud-backup").item(0) as Element
        val transfer = extraction.getElementsByTagName("device-transfer").item(0) as Element
        for (section in listOf(cloud, transfer)) {
            assertEquals(setOf(sessionPath, crashPath), fileExcludes(section))
        }
        assertEquals(setOf(sessionPath, crashPath), fileExcludes(fullBackup.documentElement))

        val manifest = parseXml("src/main/AndroidManifest.xml").getElementsByTagName("application").item(0) as Element
        assertEquals("@xml/data_extraction_rules", manifest.getAttribute("android:dataExtractionRules"))
        assertEquals("@xml/backup_rules", manifest.getAttribute("android:fullBackupContent"))
        assertEquals("true", manifest.getAttribute("android:allowBackup"))
    }

    private fun parseXml(path: String): Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))

    private fun fileExcludes(parent: Element): Set<String> {
        val excludes = parent.getElementsByTagName("exclude")
        return (0 until excludes.length)
            .map { excludes.item(it) as Element }
            .filter { it.getAttribute("domain") == "file" }
            .map { it.getAttribute("path") }
            .toSet()
    }

    @Test
    fun breakActivityExcludedFromRecents() {
        val manifest = parseXml("src/main/AndroidManifest.xml")
        val activities = manifest.getElementsByTagName("activity")
        val breakActivity = (0 until activities.length)
            .map { activities.item(it) as Element }
            .single { it.getAttribute("android:name") == ".ui.breakui.BreakActivity" }
        assertEquals("true", breakActivity.getAttribute("android:excludeFromRecents"))
        assertTrue(breakActivity.hasAttribute("android:taskAffinity"))
        assertEquals("", breakActivity.getAttribute("android:taskAffinity"))
    }

    @Test
    fun testBootReceiverIntentFilters() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED).setPackage(context.packageName)
        val packageReplacedIntent = Intent(Intent.ACTION_MY_PACKAGE_REPLACED).setPackage(context.packageName)

        val bootReceivers = pm.queryBroadcastReceivers(bootIntent, 0)
        val packageReplacedReceivers = pm.queryBroadcastReceivers(packageReplacedIntent, 0)

        assertTrue(bootReceivers.any { it.activityInfo.name == BootReceiver::class.java.name })
        assertTrue(packageReplacedReceivers.any { it.activityInfo.name == BootReceiver::class.java.name })
    }
}
