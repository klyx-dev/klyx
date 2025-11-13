package com.klyx.viewmodel

import com.klyx.core.file.KxFile
import com.klyx.editor.lsp.LanguageServerManager
import com.klyx.extension.api.Worktree
import com.klyx.extension.api.parentAsWorktreeOrSelf

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
