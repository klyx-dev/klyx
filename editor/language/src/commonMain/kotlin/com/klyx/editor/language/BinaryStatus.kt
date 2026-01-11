package com.klyx.editor.language

sealed interface BinaryStatus {
    data object None : BinaryStatus
    data object CheckingForUpdate : BinaryStatus
    data object Downloading : BinaryStatus
    data object Starting : BinaryStatus
    data object Stopping : BinaryStatus
    data object Stopped : BinaryStatus
    data class Failed(val error: String) : BinaryStatus
}
