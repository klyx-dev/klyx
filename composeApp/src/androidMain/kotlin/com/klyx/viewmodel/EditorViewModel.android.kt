package com.klyx.viewmodel

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.klyx.core.file.KxFile
import com.klyx.editor.lsp.LanguageServerManager
import com.klyx.extension.api.Worktree
import com.klyx.extension.api.parentAsWorktreeOrSelf
import com.klyx.ui.component.terminal.Terminal

fun EditorViewModel.openTerminalTab() {
    val id = "term"

    openTab("Terminal", id = id) {
        Terminal(
            modifier = Modifier.fillMaxSize(),
            onSessionFinish = {
                closeTab(id)
            }
        )
    }
}

internal actual suspend fun onCloseFileTab(worktree: Worktree?, file: KxFile) {
    runCatching {
        LanguageServerManager.closeDocument(worktree ?: file.parentAsWorktreeOrSelf(), file)
    }
}

internal actual suspend fun onSaveFile(worktree: Worktree?, file: KxFile) {
    runCatching {
        LanguageServerManager.saveDocument(worktree ?: file.parentAsWorktreeOrSelf(), file)
    }
}
