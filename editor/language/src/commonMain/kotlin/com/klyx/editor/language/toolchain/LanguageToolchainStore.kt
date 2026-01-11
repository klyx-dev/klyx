package com.klyx.editor.language.toolchain

import com.klyx.core.app.App
import com.klyx.editor.language.LanguageName
import com.klyx.settings.WorktreeId
import okio.Path

interface LanguageToolchainStore {
    suspend fun activeToolchain(
        worktreeId: WorktreeId,
        relativePath: Path,
        languageName: LanguageName,
        cx: App
    ): Toolchain?
}
