package `in`.rahulja.vision202020.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VisionTimerStateHolder {
    private val _state = MutableStateFlow<TimerStatus>(TimerStatus.Idle)
    val state: StateFlow<TimerStatus> = _state.asStateFlow()

    fun setState(status: TimerStatus) {
        _state.value = status
    }
}
