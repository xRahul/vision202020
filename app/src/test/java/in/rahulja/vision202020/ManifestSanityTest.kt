package `in`.rahulja.vision202020

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.service.BootReceiver
import `in`.rahulja.vision202020.ui.breakui.BreakActivity
import `in`.rahulja.vision202020.ui.main.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
