package `in`.rahulja.vision202020.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHelperFormatTimeTest {

    @Test
    fun `formatTime formats minutes and seconds with leading zeros`() {
        assertEquals("00:00", NotificationHelper.formatTime(0))
        assertEquals("00:05", NotificationHelper.formatTime(5))
        assertEquals("01:30", NotificationHelper.formatTime(90))
        assertEquals("20:00", NotificationHelper.formatTime(1200))
        assertEquals("19:59", NotificationHelper.formatTime(1199))
    }

    @Test
    fun `formatTime uses ASCII digits regardless of default locale`() {
        val result = NotificationHelper.formatTime(1200)
        assertTrue(result.all { it.isDigit() || it == ':' })
        assertEquals("20:00", result)
    }

    @Test
    fun `formatTime handles single-digit seconds with leading zero`() {
        assertEquals("00:07", NotificationHelper.formatTime(7))
    }

    @Test
    fun `formatTime handles exactly one hour`() {
        assertEquals("60:00", NotificationHelper.formatTime(3600))
    }

    @Test
    fun `formatTime handles 99 minutes and 59 seconds`() {
        assertEquals("99:59", NotificationHelper.formatTime(5999))
    }
}