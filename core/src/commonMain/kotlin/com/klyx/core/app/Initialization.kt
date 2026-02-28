package com.klyx.core.app

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class InitializationState(
    val isComplete: Boolean = false,
    val error: Throwable? = null,
)

object Initialization {
    val state: StateFlow<InitializationState>
        field = MutableStateFlow(InitializationState())

    fun complete() {
        state.update { it.copy(isComplete = true) }
    }
}
