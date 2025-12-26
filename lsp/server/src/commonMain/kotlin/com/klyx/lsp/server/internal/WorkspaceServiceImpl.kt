package com.klyx.lsp.server.internal

import com.klyx.lsp.CreateFilesParams
import com.klyx.lsp.DeleteFilesParams
import com.klyx.lsp.DidChangeConfigurationParams
import com.klyx.lsp.DidChangeWatchedFilesParams
import com.klyx.lsp.DidChangeWorkspaceFoldersParams
import com.klyx.lsp.ExecuteCommandParams
import com.klyx.lsp.RenameFilesParams
import com.klyx.lsp.SymbolInformation
import com.klyx.lsp.TextDocumentContentParams
import com.klyx.lsp.TextDocumentContentResult
import com.klyx.lsp.WorkspaceDiagnosticParams
import com.klyx.lsp.WorkspaceDiagnosticReport
import com.klyx.lsp.WorkspaceEdit
import com.klyx.lsp.WorkspaceSymbol
import com.klyx.lsp.WorkspaceSymbolParams
import com.klyx.lsp.server.WorkspaceService
import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.OneOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

internal class WorkspaceServiceImpl(val connection: JsonRpcConnection, val json: Json) : WorkspaceService {

    private suspend inline fun <reified T, reified Params> sendRequest(method: String, params: Params? = null): T {
        return connection.sendRequest("workspace/$method", params)
    }

    private suspend inline fun <reified Params> sendNotification(method: String, params: Params? = null) {
        connection.sendNotification("workspace/$method", json.encodeToJsonElement(params))
    }

    override suspend fun diagnostic(params: WorkspaceDiagnosticParams): WorkspaceDiagnosticReport {
        return sendRequest("diagnostic", params)
    }

    @Suppress("DEPRECATION")
    override suspend fun symbol(params: WorkspaceSymbolParams): OneOf<List<SymbolInformation>, List<WorkspaceSymbol>>? {
        return sendRequest("symbol", params)
    }

    override suspend fun resolveWorkspaceSymbol(symbol: WorkspaceSymbol): WorkspaceSymbol {
        return connection.sendRequest("workspaceSymbol/resolve", symbol)
    }

    override suspend fun didChangeConfiguration(params: DidChangeConfigurationParams) {
        sendNotification("didChangeConfiguration", params)
    }

    override suspend fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams) {
        sendNotification("didChangeWorkspaceFolders", params)
    }

    override suspend fun willCreateFiles(params: CreateFilesParams): WorkspaceEdit? {
        return sendRequest("willCreateFiles", params)
    }

    override suspend fun didCreateFiles(params: CreateFilesParams) {
        sendNotification("didCreateFiles", params)
    }

    override suspend fun willRenameFiles(params: RenameFilesParams): WorkspaceEdit? {
        return sendRequest("willRenameFiles", params)
    }

    override suspend fun didRenameFiles(params: RenameFilesParams) {
        sendNotification("didRenameFiles", params)
    }

    override suspend fun willDeleteFiles(params: DeleteFilesParams): WorkspaceEdit? {
        return sendRequest("willDeleteFiles", params)
    }

    override suspend fun didDeleteFiles(params: DeleteFilesParams) {
        sendNotification("didDeleteFiles", params)
    }

    override suspend fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
        sendNotification("didChangeWatchedFiles", params)
    }

    override suspend fun executeCommand(params: ExecuteCommandParams): LSPAny {
        return sendRequest("executeCommand", params)
    }

    override suspend fun textDocumentContent(params: TextDocumentContentParams): TextDocumentContentResult {
        return sendRequest("textDocumentContent", params)
    }
}
