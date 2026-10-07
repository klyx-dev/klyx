@file:OptIn(ExperimentalAtomicApi::class)

package com.klyx.lsp.server.internal

import com.klyx.lsp.InitializeParams
import com.klyx.lsp.InitializeResult
import com.klyx.lsp.InitializedParams
import com.klyx.lsp.SetTraceParams
import com.klyx.lsp.WorkDoneProgressCancelParams
import com.klyx.lsp.server.LanguageClient
import com.klyx.lsp.server.LanguageServer
import com.klyx.lsp.server.ServerAlreadyInitializedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal fun LanguageServerImpl(
    scope: CoroutineScope,
    client: LanguageClient,
    stdout: RawSource,
    stdin: RawSink
): LanguageServerImpl {
    val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    val scope = scope + SupervisorJob()
    val connection = JsonRpcConnection(stdout, stdin, client, json, scope)
    return LanguageServerImpl(connection, json)
}

internal class LanguageServerImpl(val connection: JsonRpcConnection, val json: Json) : LanguageServer {

    override val textDocument = TextDocumentServiceImpl(connection, json)
    override val workspace = WorkspaceServiceImpl(connection, json)
    override val notebookDocument = NotebookDocumentServiceImpl(connection, json)

    private val isInitialized = AtomicBoolean(false)
    private val isShutdown = AtomicBoolean(false)

    init {
        connection.start()
    }

    override suspend fun initialize(params: InitializeParams): InitializeResult {
        if (isInitialized.load()) {
            throw ServerAlreadyInitializedException()
        }

        return connection.sendRequest("initialize", params)
    }

    override suspend fun initialized(params: InitializedParams) {
        connection.sendNotification("initialized", json.encodeToJsonElement(params))
        isInitialized.store(true)
    }

    override suspend fun setTrace(params: SetTraceParams) {
        connection.sendNotification("$/setTrace", json.encodeToJsonElement(params))
    }

    override suspend fun shutdown() {
        val _ = connection.sendRequest("shutdown")
        isShutdown.store(true)
    }

    override suspend fun exit() {
        connection.sendNotification("exit")
    }

    override suspend fun cancelProgress(params: WorkDoneProgressCancelParams) {
        connection.sendNotification("window/workDoneProgress/cancel", json.encodeToJsonElement(params))
    }
}
