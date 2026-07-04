package com.klyx.event

sealed interface UiEvent {
    data class ShowError(val error: String) : UiEvent
    data class ShowMessage(val message: String) : UiEvent
}
