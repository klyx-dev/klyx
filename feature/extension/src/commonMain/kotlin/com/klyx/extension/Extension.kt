package com.klyx.extension

import com.klyx.core.lsp.LanguageServerName
import com.klyx.extension.native.ProjectDelegate
import com.klyx.extension.native.WorktreeDelegate
import com.klyx.extension.types.Completion
import com.klyx.extension.types.ContextServerConfiguration
import com.klyx.extension.types.SlashCommand
import com.klyx.extension.types.SlashCommandArgumentCompletion
import com.klyx.extension.types.SlashCommandOutput
import com.klyx.extension.types.Symbol
import okio.Path

interface Extension {
    /**
     * Returns the [ExtensionManifest] for this extension.
     */
    fun manifest(): ExtensionManifest

    /**
     * Returns the path to this extension's working directory.
     */
    fun workDir(): Path

    /**
     * Returns a path relative to this extension's working directory.
     */
    fun pathFromExtension(path: Path): Path {
        return (workDir() / path).normalized()
    }

    suspend fun languageServerCommand(
        languageServerId: LanguageServerName,
        worktree: WorktreeDelegate
    ): Result<Command>

    suspend fun languageServerInitializationOptions(
        languageServerId: LanguageServerName,
        worktree: WorktreeDelegate
    ): Result<String?>

    suspend fun languageServerWorkspaceConfiguration(
        languageServerId: LanguageServerName,
        worktree: WorktreeDelegate
    ): Result<String?>

    suspend fun languageServerAdditionalInitializationOptions(
        languageServerId: LanguageServerName,
        targetLanguageServerId: LanguageServerName,
        worktree: WorktreeDelegate
    ): Result<String?>

    suspend fun languageServerAdditionalWorkspaceConfiguration(
        languageServerId: LanguageServerName,
        targetLanguageServerId: LanguageServerName,
        worktree: WorktreeDelegate
    ): Result<String?>

    suspend fun labelsForCompletions(
        languageServerId: LanguageServerName,
        completions: List<Completion>
    ): Result<List<CodeLabel?>>

    suspend fun labelsForSymbols(
        languageServerId: LanguageServerName,
        symbols: List<Symbol>
    ): Result<List<CodeLabel?>>

    suspend fun completeSlashCommandArgument(
        command: SlashCommand,
        arguments: List<String>
    ): Result<List<SlashCommandArgumentCompletion>>

    suspend fun runSlashCommand(
        command: SlashCommand,
        arguments: List<String>,
        worktree: WorktreeDelegate?
    ): Result<SlashCommandOutput>

    suspend fun contextServerCommand(
        contextServerId: String,
        project: ProjectDelegate
    ): Result<Command>

    suspend fun contextServerConfiguration(
        contextServerId: String,
        project: ProjectDelegate
    ): Result<ContextServerConfiguration?>
}
