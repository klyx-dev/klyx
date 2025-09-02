package com.klyx.viewmodel

import com.klyx.core.file.KxFile
import com.klyx.extension.api.Worktree

internal actual suspend fun onCloseFileTab(
    worktree: Worktree?,
    file: KxFile
) {
}
