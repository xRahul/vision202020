package `in`.rahulja.vision202020.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * The single sequential lane for every session save/clear in the app, so a fast
 * break->complete transition (or a boot-time clear) cannot be overtaken by an older write.
 */
object SessionWriteLane {
    @OptIn(ExperimentalCoroutinesApi::class)
    val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
}
