package com.vision202020.app

import org.junit.Assert.assertNotNull
import org.junit.Test

class ManifestSanityTest {
    @Test
    fun testPackageConfiguration() {
        val appClass = VisionApplication::class.java
        assertNotNull(appClass)
    }
}
