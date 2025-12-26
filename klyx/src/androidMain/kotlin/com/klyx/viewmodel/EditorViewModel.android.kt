package com.klyx.viewmodel

import com.klyx.core.file.KxFile
import com.klyx.core.file.Worktree
import com.klyx.core.file.parentAsWorktreeOrSelf
import com.klyx.editor.lsp.LanguageServerManager

internal actual suspend fun onCloseFileTab(worktree: Worktree?, file: KxFile) {
    val _ = runCatching { LanguageServerManager.closeDocument(worktree ?: file.parentAsWorktreeOrSelf(), file) }
}

internal actual suspend fun onSaveFile(worktree: Worktree?, file: KxFile) {
    val _ = runCatching { LanguageServerManager.saveDocument(worktree ?: file.parentAsWorktreeOrSelf(), file) }
}
