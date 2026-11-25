package `in`.rahulja.vision202020.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import `in`.rahulja.vision202020.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AboutLibrariesMetadataTest {

    @Test
    fun generatedLicenseMetadataIsPresent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val metadata = context.resources.openRawResource(R.raw.aboutlibraries)
            .bufferedReader()
            .use { it.readText() }

        assertTrue("SBOM/license metadata should not be empty", metadata.isNotBlank())
        assertTrue(
            "License metadata should list at least one library",
            metadata.contains("\"libraries\"")
        )
    }
}
