@file:OptIn(ExperimentalCodeEditorApi::class)

package com.klyx.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.klyx.core.logging.Message
import com.klyx.editor.CursorState
import com.klyx.editor.ExperimentalCodeEditorApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Stable
enum class LspState {
    Idle,
    Indexing,
    Ready,
    Error,
}

@Stable
data class StatusBarState(
    val readOnly: Boolean = false,
    val language: String? = null,
    val cursorState: CursorState? = null,
    val lspState: LspState = LspState.Idle,
    val errorCount: Int = 0,
    val warningCount: Int = 0,
    val encoding: String = "UTF-8",
    val lineEndings: String = "LF",
    val insertMode: Boolean = true,
)

@Stable
data class LogState(
    val message: Message? = null,
    val isProgressive: Boolean = false
)

class StatusBarViewModel : ViewModel() {

    val state: StateFlow<StatusBarState>
        field = MutableStateFlow(StatusBarState())

    val currentLogState: StateFlow<LogState>
        field = MutableStateFlow(LogState())

    fun setCurrentLogMessage(message: Message?, isProgressive: Boolean = false) {
        currentLogState.update { it.copy(message = message, isProgressive = isProgressive) }
    }

    fun setReadOnly(readOnly: Boolean) {
        state.update { it.copy(readOnly = readOnly) }
    }

    fun setLanguage(language: String?) {
        state.update { it.copy(language = language) }
    }

    fun setCursorState(cursorState: CursorState?) {
        state.update { it.copy(cursorState = cursorState) }
    }

    fun setLspState(lspState: LspState) {
        state.update { it.copy(lspState = lspState) }
    }

    fun setDiagnostics(errorCount: Int, warningCount: Int) {
        state.update { it.copy(errorCount = errorCount, warningCount = warningCount) }
    }

    fun setEncoding(encoding: String) {
        state.update { it.copy(encoding = encoding) }
    }

    fun setLineEndings(lineEndings: String) {
        state.update { it.copy(lineEndings = lineEndings) }
    }

    fun setInsertMode(insertMode: Boolean) {
        state.update { it.copy(insertMode = insertMode) }
    }
}
