package `in`.rahulja.vision202020.service

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CancellableRunCatchingTest {

    @Test
    fun rethrowsCancellation() {
        val cancellation = CancellationException("cancelled")
        try {
            runCatchingCancellable { throw cancellation }
            fail("CancellationException must propagate")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
    }

    @Test
    fun capturesOtherExceptions() {
        val boom = IllegalStateException("boom")
        val result = runCatchingCancellable { throw boom }
        assertTrue(result.isFailure)
        assertSame(boom, result.exceptionOrNull())
    }

    @Test
    fun returnsSuccessValue() {
        assertEquals(7, runCatchingCancellable { 7 }.getOrNull())
    }
}
