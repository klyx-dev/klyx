package com.klyx.lsp.server.internal

import com.klyx.lsp.InitializeParams
import com.klyx.lsp.InitializeResult
import com.klyx.lsp.InitializedParams
import com.klyx.lsp.SetTraceParams
import com.klyx.lsp.WorkDoneProgressCancelParams
import com.klyx.lsp.server.LanguageClient
import com.klyx.lsp.server.LanguageServer
import com.klyx.lsp.server.ServerAlreadyInitializedException
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

internal fun LanguageServerImpl(
    scope: CoroutineScope,
    client: LanguageClient,
    stdin: RawSource,
    stdout: RawSink
): LanguageServerImpl {
    val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    val scope = scope + SupervisorJob()
    val connection = JsonRpcConnection(stdin, stdout, client, json, scope)
    return LanguageServerImpl(connection, json)
}

internal class LanguageServerImpl(val connection: JsonRpcConnection, val json: Json) : LanguageServer {

    override val textDocument = TextDocumentServiceImpl(connection, json)
    override val workspace = WorkspaceServiceImpl(connection, json)
    override val notebookDocument = NotebookDocumentServiceImpl(connection, json)

    private var isInitialized by atomic(false)
    private var isShutdown by atomic(false)

    init {
        connection.start()
    }

    override suspend fun initialize(params: InitializeParams): InitializeResult {
        if (isInitialized) {
            throw ServerAlreadyInitializedException()
        }

        return connection.sendRequest("initialize", params)
    }

    override suspend fun initialized(params: InitializedParams) {
        connection.sendNotification("initialized", json.encodeToJsonElement(params))
        isInitialized = true
    }

    override suspend fun setTrace(params: SetTraceParams) {
        connection.sendNotification("$/setTrace", json.encodeToJsonElement(params))
    }

    override suspend fun shutdown() {
        connection.sendRequest("shutdown")
        isShutdown = true
    }

    override suspend fun exit() {
        connection.sendNotification("exit")
    }

    override suspend fun cancelProgress(params: WorkDoneProgressCancelParams) {
        connection.sendNotification("window/workDoneProgress/cancel", json.encodeToJsonElement(params))
    }
}
