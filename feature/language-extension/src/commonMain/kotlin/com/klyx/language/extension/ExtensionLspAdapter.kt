package com.klyx.language.extension

import arrow.core.raise.result
import com.klyx.core.lsp.LanguageServerBinary
import com.klyx.core.lsp.LanguageServerBinaryOptions
import com.klyx.core.lsp.LanguageServerName
import com.klyx.core.unreachable
import com.klyx.core.util.asHashMap
import com.klyx.core.util.emptyJsonObject
import com.klyx.core.util.intoPath
import com.klyx.editor.language.LanguageName
import com.klyx.editor.language.LanguageServerBinaryLocations
import com.klyx.editor.language.LspAdapter
import com.klyx.editor.language.LspAdapterDelegate
import com.klyx.editor.language.ServerBinaryCache
import com.klyx.extension.Extension
import com.klyx.extension.native.ReadTextFileException
import com.klyx.extension.native.WorktreeDelegate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import okio.Path
import okio.Path.Companion.toPath

internal class WorktreeDelegateAdapter(private val delegate: LspAdapterDelegate) : WorktreeDelegate {
    override fun id() = delegate.worktreeId().value
    override fun rootPath() = delegate.worktreeRootPath().toString()

    override suspend fun readTextFile(path: String): String {
        return try {
            delegate.readTextFile(path.toPath()).getOrThrow()
        } catch (e: Throwable) {
            throw ReadTextFileException(e.stackTraceToString(), e)
        }
    }

    override suspend fun which(binaryName: String) = delegate.which(binaryName)?.toString()
    override suspend fun shellEnv() = delegate.shellEnv()
}

class ExtensionLspAdapter(val extension: Extension, val languageServerId: LanguageServerName) : LspAdapter {
    override fun name() = languageServerId
    override val isExtension: Boolean = true

    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun tryFetchServerBinary(
        delegate: LspAdapterDelegate,
        containerDir: Path,
        preRelease: Boolean
    ): Result<LanguageServerBinary> {
        unreachable("getLanguageServerCommand is overridden")
    }

    override fun getLanguageServerCommand(
        delegate: LspAdapterDelegate,
        binaryOptions: LanguageServerBinaryOptions,
        cachedBinary: ServerBinaryCache?
    ): LanguageServerBinaryLocations {
        return suspend {
            val ret = result {
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

    override fun languageIds(): Map<LanguageName, String> {
        return extension
            .manifest()
            .languageServers[languageServerId]
            ?.languageIds.orEmpty()
    }

    override suspend fun initializationOptions(delegate: LspAdapterDelegate): Result<JsonObject?> {
        return result {
            val delegate = WorktreeDelegateAdapter(delegate)
            val jsonOptions = extension
                .languageServerInitializationOptions(languageServerId, delegate).bind()

            jsonOptions?.let {
                try {
                    val obj: JsonObject = json.decodeFromString(jsonOptions)

                    JsonObject(
                        obj.mapValues { (_, value) ->
                            if (value is JsonNull) emptyJsonObject() else value
                        }
                    )
                } catch (e: Throwable) {
                    raise(RuntimeException("failed to parse initialization_options from extension: $jsonOptions", e))
                }
            }
        }
    }

    override suspend fun workspaceConfiguration(delegate: LspAdapterDelegate): Result<JsonObject> {
        return result {
            val delegate = WorktreeDelegateAdapter(delegate)

            val jsonOptions = extension
                .languageServerWorkspaceConfiguration(languageServerId, delegate).bind()

            jsonOptions?.let {
                try {
                    val obj: JsonObject = json.decodeFromString(jsonOptions)

                    JsonObject(
                        obj.mapValues { (_, value) ->
                            if (value is JsonNull) emptyJsonObject() else value
                        }
                    )
                } catch (e: Throwable) {
                    raise(RuntimeException("failed to parse workspace_configuration from extension: $jsonOptions", e))
                }
            } ?: emptyJsonObject()
        }
    }
}
