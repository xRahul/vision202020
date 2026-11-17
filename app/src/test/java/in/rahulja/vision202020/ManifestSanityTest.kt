package `in`.rahulja.vision202020

import org.junit.Assert.assertNotNull
import org.junit.Test

class ManifestSanityTest {
    @Test
    fun testPackageConfiguration() {
        val appClass = VisionApplication::class.java
        assertNotNull(appClass)
    }
}
