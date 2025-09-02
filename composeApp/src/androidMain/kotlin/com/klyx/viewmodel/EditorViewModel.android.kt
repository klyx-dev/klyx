package com.klyx.viewmodel

import com.klyx.core.file.KxFile
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.editor.lsp.LanguageServerManager
import com.klyx.extension.api.Worktree
import com.klyx.extension.api.parentAsWorktreeOrSelf
import com.klyx.tab.Tab
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCodeEditorApi::class)
fun EditorViewModel.getActiveEditor() = activeTab.map {
    when (val tab = it) {
        is Tab.FileTab -> tab.editorState.editor
        else -> null
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
