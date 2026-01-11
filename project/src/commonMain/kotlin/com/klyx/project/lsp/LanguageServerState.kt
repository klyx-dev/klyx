package com.klyx.project.lsp

import com.klyx.editor.language.LspAdapter
import com.klyx.lsp.server.LanguageServer
import kotlinx.coroutines.CompletableDeferred

sealed interface LanguageServerState {
    data class Starting(val startup: CompletableDeferred<LanguageServer>) : LanguageServerState
    data class Running(val server: LanguageServer, val adapter: LspAdapter) : LanguageServerState
    data class Failed(val error: Throwable) : LanguageServerState
    data object Stopped : LanguageServerState
}
