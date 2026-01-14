package com.klyx.project.lsp

import com.klyx.core.file.toKotlinxIoPath
import com.klyx.core.io.okioFs
import com.klyx.core.logging.logger
import com.klyx.core.lsp.LanguageServerName
import com.klyx.core.util.asHashMap
import com.klyx.editor.language.BinaryStatus
import com.klyx.editor.language.LanguageRegistry
import com.klyx.editor.language.LspAdapterDelegate
import com.klyx.project.Project
import com.klyx.project.Worktree
import com.klyx.settings.WorktreeId
import okio.Path
import okio.Path.Companion.toPath

class LocalLspAdapterDelegate(
    val worktree: Worktree,
    val languageRegistry: LanguageRegistry
) : LspAdapterDelegate {
    override fun worktreeId(): WorktreeId {
        return WorktreeId(worktree.id())
    }

    override fun worktreeRootPath(): Path {
        return worktree.rootFile.absolutePath.toPath()
    }

    override fun updateStatus(
        language: LanguageServerName,
        status: BinaryStatus
    ) {
        val logger = logger(language)

        when (status) {
            BinaryStatus.CheckingForUpdate -> logger.progress { "[$language] checking for updates" }
            BinaryStatus.Downloading -> logger.progress { "downloading language server '$language'" }
            is BinaryStatus.Failed -> logger.error { "[$language] failed: ${status.error}" }
            BinaryStatus.None -> logger.info { "" }
            BinaryStatus.Starting -> logger.progress { "starting" }
            BinaryStatus.Stopped -> logger.info { "" }
            BinaryStatus.Stopping -> logger.progress { "stopping" }
        }
    }

    override suspend fun languageServerDownloadDir(name: LanguageServerName): Path? {
        val dir = languageRegistry.languageServerDownloadDir(name) ?: return null

        if (!okioFs.exists(dir)) {
            okioFs.createDirectories(dir)
        }

        return dir
    }

    override suspend fun readTextFile(path: Path) = worktree.readTextFile(path.toKotlinxIoPath())

    override suspend fun which(command: String): Path? {
        return worktree.which(command)?.toPath()
    }

    override suspend fun shellEnv() = worktree.shellEnv().asHashMap()
}

fun makeLspAdapterDelegate(project: Project): LspAdapterDelegate? = project.worktrees.firstOrNull()?.let { worktree ->
    LocalLspAdapterDelegate(worktree, LanguageRegistry.INSTANCE)
}

fun makeLspAdapterDelegate(worktree: Worktree): LspAdapterDelegate =
    LocalLspAdapterDelegate(worktree, LanguageRegistry.INSTANCE)
