package `in`.rahulja.vision202020.service

import kotlinx.coroutines.CancellationException

/** Like [runCatching], but rethrows [CancellationException] so cancelled coroutines still stop. */
internal inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
