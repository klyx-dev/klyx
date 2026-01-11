package com.klyx.extension.host

import arrow.core.raise.context.result
import com.klyx.core.file.resolve
import com.klyx.core.file.toKxFile
import com.klyx.core.file.toOkioPath
import com.klyx.core.io.okioFs
import com.klyx.core.lsp.LanguageServerName
import com.klyx.extension.CodeLabel
import com.klyx.extension.Command
import com.klyx.extension.Extension
import com.klyx.extension.ExtensionManifest
import com.klyx.extension.native.ExtensionLoadException
import com.klyx.extension.native.Manifest
import com.klyx.extension.native.ProjectDelegate
import com.klyx.extension.native.WasmExtensionWrapper
import com.klyx.extension.native.WasmHost
import com.klyx.extension.native.WorktreeDelegate
import com.klyx.extension.types.Completion
import com.klyx.extension.types.ContextServerConfiguration
import com.klyx.extension.types.SlashCommand
import com.klyx.extension.types.SlashCommandArgumentCompletion
import com.klyx.extension.types.SlashCommandOutput
import com.klyx.extension.types.Symbol
import io.github.z4kn4fein.semver.Version
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer

class WasmExtension(
    private val wrapper: WasmExtensionWrapper,
    private val manifest: ExtensionManifest,
) : Extension, AutoCloseable {
    val klyxApiVersion: Version = wrapper.klyxApiVersion()

    override fun manifest() = manifest
    override fun workDir() = wrapper.workDir().toPath()

    override suspend fun languageServerCommand(
        languageServerId: LanguageServerName,
        worktree: WorktreeDelegate
    ): Result<Command> = result {
        wrapper.languageServerCommand(languageServerId, worktree)
    }

    override suspend fun languageServerInitializationOptions(
        languageServerId: LanguageServerName,
        worktree: WorktreeDelegate
    ): Result<String?> = result {
        wrapper.languageServerInitializationOptions(languageServerId, worktree)
    }

    override suspend fun languageServerWorkspaceConfiguration(
        languageServerId: LanguageServerName,
        worktree: WorktreeDelegate
    ): Result<String?> = result {
        wrapper.languageServerWorkspaceConfiguration(languageServerId, worktree)
    }

    override suspend fun languageServerAdditionalInitializationOptions(
        languageServerId: LanguageServerName,
        targetLanguageServerId: LanguageServerName,
        worktree: WorktreeDelegate
    ): Result<String?> = result {
        wrapper.languageServerAdditionalInitializationOptions(languageServerId, targetLanguageServerId, worktree)
    }

    override suspend fun languageServerAdditionalWorkspaceConfiguration(
        languageServerId: LanguageServerName,
        targetLanguageServerId: LanguageServerName,
        worktree: WorktreeDelegate
    ): Result<String?> = result {
        wrapper.languageServerAdditionalWorkspaceConfiguration(languageServerId, targetLanguageServerId, worktree)
    }

    override suspend fun labelsForCompletions(
        languageServerId: LanguageServerName,
        completions: List<Completion>
    ): Result<List<CodeLabel?>> = result {
        wrapper.labelsForCompletions(languageServerId, completions)
    }

    override suspend fun labelsForSymbols(
        languageServerId: LanguageServerName,
        symbols: List<Symbol>
    ): Result<List<CodeLabel?>> = result {
        wrapper.labelsForSymbols(languageServerId, symbols)
    }

    override suspend fun completeSlashCommandArgument(
        command: SlashCommand,
        arguments: List<String>
    ): Result<List<SlashCommandArgumentCompletion>> {
        TODO("Not yet implemented")
    }

    override suspend fun runSlashCommand(
        command: SlashCommand,
        arguments: List<String>,
        worktree: WorktreeDelegate?
    ): Result<SlashCommandOutput> {
        TODO("Not yet implemented")
    }

    override suspend fun contextServerCommand(
        contextServerId: String,
        project: ProjectDelegate
    ): Result<Command> {
        TODO("Not yet implemented")
    }

    override suspend fun contextServerConfiguration(
        contextServerId: String,
        project: ProjectDelegate
    ): Result<ContextServerConfiguration?> {
        TODO("Not yet implemented")
    }

    override fun close() {
        wrapper.close()
    }

    companion object {
        @Throws(ExtensionLoadException::class, CancellationException::class)
        suspend fun load(extensionDir: Path, manifest: ExtensionManifest, host: WasmHost) =
            withContext(Dispatchers.IO) {
                val wasmFiles = listOf("src", "lib")
                    .flatMap { subDir ->
                        extensionDir.toKxFile().resolve(subDir)
                            .listFiles { it.extension == "wasm" }
                            ?.toList().orEmpty()
                    }
                val path = wasmFiles.firstOrNull()?.toOkioPath() ?: extensionDir.resolve("extension.wasm")
                val wasmBytes = okioFs.source(path).buffer().use { it.readByteArray() }
                val wrapper = host.loadExtension(
                    wasmBytes = wasmBytes,
                    manifest = manifest.native(),
                    capabilityGranter = ExtensionCapabilityGranter(DefaultGrantedCapabilities, manifest)
                )
                WasmExtension(wrapper, manifest)
            }
    }
}

private fun ExtensionManifest.native() = Manifest(id, name, version, description, repository)
