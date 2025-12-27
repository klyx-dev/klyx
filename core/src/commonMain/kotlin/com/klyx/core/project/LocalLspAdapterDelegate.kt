package com.klyx.core.project

import com.klyx.core.file.Project
import com.klyx.core.file.Worktree
import com.klyx.core.file.WorktreeId
import com.klyx.core.io.fs
import com.klyx.core.io.intoPath
import com.klyx.core.language.BinaryStatus
import com.klyx.core.language.LanguageRegistry
import com.klyx.core.language.LspAdapterDelegate
import com.klyx.core.logging.logger
import com.klyx.core.lsp.LanguageServerName
import com.klyx.core.util.asHashMap
import kotlinx.io.files.Path

class LocalLspAdapterDelegate(
    val worktree: Worktree,
    val languageRegistry: LanguageRegistry
) : LspAdapterDelegate {
    override fun worktreeId(): WorktreeId {
        return worktree.id()
    }

    override fun worktreeRootPath(): Path {
        return worktree.rootFile.absolutePath.intoPath()
    }

    override fun updateStatus(
        language: LanguageServerName,
        status: BinaryStatus
    ) {
        val logger = logger(language)

        when(status) {
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

        if (!fs.exists(dir)) {
            fs.createDirectories(dir)
        }

        return dir
    }

    override suspend fun readTextFile(path: Path) = worktree.readTextFile(path)

    override suspend fun which(command: String): Path? {
        return worktree.which(command)?.intoPath()
    }

    override suspend fun shellEnv() = worktree.shellEnv().asHashMap()
}

fun makeLspAdapterDelegate(project: Project): LspAdapterDelegate? = project.worktrees.firstOrNull()?.let { worktree ->
    LocalLspAdapterDelegate(worktree, LanguageRegistry.INSTANCE)
}

fun makeLspAdapterDelegate(worktree: Worktree): LspAdapterDelegate =
    LocalLspAdapterDelegate(worktree, LanguageRegistry.INSTANCE)
