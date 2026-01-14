package com.klyx.viewmodel

import com.klyx.core.file.KxFile
import com.klyx.editor.lsp.LanguageServerManager
import com.klyx.project.Worktree
import com.klyx.project.parentAsWorktreeOrSelf

internal actual suspend fun onCloseFileTab(worktree: Worktree?, file: KxFile) {
    val _ = runCatching { LanguageServerManager.closeDocument(worktree ?: file.parentAsWorktreeOrSelf(), file) }
}

internal actual suspend fun onSaveFile(worktree: Worktree?, file: KxFile) {
    val _ = runCatching { LanguageServerManager.saveDocument(worktree ?: file.parentAsWorktreeOrSelf(), file) }
}
