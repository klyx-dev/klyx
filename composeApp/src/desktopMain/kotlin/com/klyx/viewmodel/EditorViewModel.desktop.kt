package com.klyx.viewmodel

import com.klyx.core.file.KxFile
import com.klyx.core.file.Worktree

internal actual suspend fun onCloseFileTab(
    worktree: Worktree?,
    file: KxFile
) {
}

internal actual suspend fun onSaveFile(worktree: Worktree?, file: KxFile) {
}
