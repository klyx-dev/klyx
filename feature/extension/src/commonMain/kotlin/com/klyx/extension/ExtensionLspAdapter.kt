package com.klyx.extension

import arrow.core.None
import arrow.core.Some
import com.klyx.core.extension.WorktreeDelegate
import com.klyx.core.io.intoPath
import com.klyx.core.language.LanguageName
import com.klyx.core.language.LanguageServerBinaryLocations
import com.klyx.core.language.LspAdapter
import com.klyx.core.language.LspAdapterDelegate
import com.klyx.core.language.ServerBinaryCache
import com.klyx.core.lsp.LanguageServerBinary
import com.klyx.core.lsp.LanguageServerBinaryOptions
import com.klyx.core.lsp.LanguageServerName
import com.klyx.core.util.asHashMap
import com.klyx.core.util.emptyJsonObject
import com.klyx.core.unreachable
import io.itsvks.anyhow.AnyhowResult
import io.itsvks.anyhow.anyhow
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * An adapter that allows an [LspAdapterDelegate] to be used as a [WorktreeDelegate].
 */
private class WorktreeDelegateAdapter(val delegate: LspAdapterDelegate) : WorktreeDelegate {
    override fun id() = delegate.worktreeId()
    override fun rootPath() = delegate.worktreeRootPath().toString()

    override suspend fun readTextFile(path: Path) = delegate.readTextFile(path)
    override suspend fun which(binaryName: String) = delegate.which(binaryName)?.toString()
    override suspend fun shellEnv() = delegate.shellEnv()
}

class ExtensionLspAdapter(
    val extension: WasmExtension,
    val languageServerId: LanguageServerName,
    val languageName: LanguageName
) : LspAdapter {
    override fun name() = languageServerId

    override val isExtension: Boolean = true

    override fun languageIds(): HashMap<LanguageName, String> {
        return extension.manifest()
            .languageServers[languageServerId]
            ?.languageIds
            ?.mapKeys { LanguageName(it.key) }
            ?.asHashMap() ?: hashMapOf()
    }

    override suspend fun initializationOptions(delegate: LspAdapterDelegate): AnyhowResult<JsonObject?> = anyhow {
        val delegate = WorktreeDelegateAdapter(delegate)
        val jsonOptions = extension
            .languageServerInitializationOptions(languageServerId, delegate).bind()

        when (jsonOptions) {
            is Some -> {
                withContext("failed to parse initialization_options from extension: ${jsonOptions.value}") {
                    Json.decodeFromString(jsonOptions.value)
                }
            }

            None -> null
        }
    }

    override suspend fun workspaceConfiguration(delegate: LspAdapterDelegate): AnyhowResult<JsonObject> = anyhow {
        val delegate = WorktreeDelegateAdapter(delegate)

        val jsonOptions = extension
            .languageServerWorkspaceConfiguration(languageServerId, delegate).bind()

        when (jsonOptions) {
            is Some -> {
                withContext("failed to parse workspace_configuration from extension: ${jsonOptions.value}") {
                    Json.decodeFromString(jsonOptions.value)
                }
            }

            None -> emptyJsonObject()
        }
    }

    override suspend fun tryFetchServerBinary(
        delegate: LspAdapterDelegate,
        containerDir: Path,
        preRelease: Boolean
    ): AnyhowResult<LanguageServerBinary> {
        unreachable("getLanguageServerCommand is overridden")
    }

    override fun getLanguageServerCommand(
        delegate: LspAdapterDelegate,
        binaryOptions: LanguageServerBinaryOptions,
        cachedBinary: ServerBinaryCache?
    ): LanguageServerBinaryLocations {
        return suspend {
            val ret = anyhow {
                val delegate = WorktreeDelegateAdapter(delegate)
                val command = extension.languageServerCommand(languageServerId, delegate).bind()

                LanguageServerBinary(
                    path = command.command.intoPath(),
                    arguments = command.args,
                    env = command.env.asHashMap()
                )
            }

            Pair(ret, null)
        }
    }
}
