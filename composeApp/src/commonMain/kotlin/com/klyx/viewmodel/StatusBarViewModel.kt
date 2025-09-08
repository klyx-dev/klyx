@file:OptIn(ExperimentalCodeEditorApi::class)

package com.klyx.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.klyx.core.logging.Message
import com.klyx.editor.CursorState
import com.klyx.editor.ExperimentalCodeEditorApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Stable
data class StatusBarState(
    val readOnly: Boolean = false,
    val language: String? = null,
    val cursorState: CursorState? = null
)

@Stable
data class LogState(
    val message: Message? = null,
    val isProgressive: Boolean = false
)

class StatusBarViewModel : ViewModel() {
    private val _state = MutableStateFlow(StatusBarState())
    val state = _state.asStateFlow()

    private val _currentLogState = MutableStateFlow(LogState())
    val currentLogState = _currentLogState.asStateFlow()

    fun setCurrentLogMessage(message: Message?, isProgressive: Boolean = false) {
        _currentLogState.update { it.copy(message = message, isProgressive = isProgressive) }
    }

    fun setReadOnly(readOnly: Boolean) {
        _state.update { it.copy(readOnly = readOnly) }
    }

    fun setLanguage(language: String?) {
        _state.update { it.copy(language = language) }
    }

    fun setCursorState(cursorState: CursorState?) {
        _state.update { it.copy(cursorState = cursorState) }
    }
}
