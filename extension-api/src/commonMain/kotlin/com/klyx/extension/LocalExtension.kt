package com.klyx.extension

import com.github.michaelbull.result.map
import com.klyx.core.DefaultScope
import com.klyx.core.extension.Extension
import com.klyx.core.logging.KxLogger
import com.klyx.extension.api.Worktree
import com.klyx.extension.api.readCommandResult
import com.klyx.extension.api.readResult
import com.klyx.extension.api.readStringOption
import com.klyx.extension.internal.Command
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmInstance
import com.klyx.wasm.WasmMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalWasmApi::class)
data class LocalExtension(
    val extension: Extension,
    val logger: KxLogger,
    internal val instance: WasmInstance,
) : AutoCloseable {
    private val memory by lazy { instance.memory }

    private val coroutineContext = Dispatchers.Default + SupervisorJob()

    /**
     * Initializes the extension.
     */
    suspend fun initialize() {
        withContext(Dispatchers.Default) {
            instance.call("init-extension")
        }
    }

    /**
     * Uninitializes the extension.
     */
    fun dispose() {
        DefaultScope.launch {
            instance.call("uninstall")
        }.invokeOnCompletion {
            close()
        }
    }

    /**
     * Returns the command used to start up the language server.
     */
    suspend fun languageServerCommand(languageServerId: String, worktree: Worktree) = withContext(coroutineContext) {
        val fn = instance.function("language-server-command")
        val result = fn(languageServerId, worktree)[0]
        memory.readCommandResult(result.asInt()).map { (command, args, env) ->
            Command(
                command = command.toString(memory),
                args = args.map { it.toString(memory) },
                env = env.associate { (k, v) -> k.toString(memory) to v.toString(memory) }
            )
        }
    }

    /**
     * Returns the initialization options to pass to the language server on startup.
     *
     * The initialization options are represented as a JSON string.
     */
    suspend fun languageServerInitializationOptions(
        languageServerId: String,
        worktree: Worktree
    ) = withContext(coroutineContext) {
        val fn = instance.function("language-server-initialization-options")
        val result = fn(languageServerId, worktree)[0]
        memory.readStringOptionResult(result.asInt())
    }

    /**
     * Returns the workspace configuration options to pass to the language server.
     */
    suspend fun languageServerWorkspaceConfiguration(
        languageServerId: String,
        worktree: Worktree
    ) = withContext(coroutineContext) {
        val fn = instance.function("language-server-workspace-configuration")
        val result = fn(languageServerId, worktree)[0]
        memory.readStringOptionResult(result.asInt())
    }

    /**
     * Returns the initialization options to pass to the other language server.
     */
    suspend fun languageServerAdditionalInitializationOptions(
        languageServerId: String,
        targetLanguageServerId: String,
        worktree: Worktree
    ) = withContext(coroutineContext) {
        val fn = instance.function("language-server-additional-initialization-options")
        val result = fn(languageServerId, targetLanguageServerId, worktree)[0]
        memory.readStringOptionResult(result.asInt())
    }

    /**
     * Returns the workspace configuration options to pass to the other language server.
     */
    suspend fun languageServerAdditionalWorkspaceConfiguration(
        languageServerId: String,
        targetLanguageServerId: String,
        worktree: Worktree
    ) = withContext(coroutineContext) {
        val fn = instance.function("language-server-additional-workspace-configuration")
        val result = fn(languageServerId, targetLanguageServerId, worktree)[0]
        memory.readStringOptionResult(result.asInt())
    }

    private fun WasmMemory.readStringOptionResult(pointer: Int) = readResult(
        pointer = pointer,
        readOk = WasmMemory::readStringOption,
        readErr = WasmMemory::readLengthPrefixedUtf8String
    )

    override fun close() {}
}
