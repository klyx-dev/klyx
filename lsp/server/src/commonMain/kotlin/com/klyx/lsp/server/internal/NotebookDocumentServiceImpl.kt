package com.klyx.lsp.server.internal

import com.klyx.lsp.DidChangeNotebookDocumentParams
import com.klyx.lsp.DidCloseNotebookDocumentParams
import com.klyx.lsp.DidOpenNotebookDocumentParams
import com.klyx.lsp.DidSaveNotebookDocumentParams
import com.klyx.lsp.server.NotebookDocumentService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

internal class NotebookDocumentServiceImpl(
    val connection: JsonRpcConnection,
    val json: Json
) : NotebookDocumentService {

    private suspend fun sendNotification(method: String, params: Any? = null) {
        connection.sendNotification("notebookDocument/$method", json.encodeToJsonElement(params))
    }

    override suspend fun didOpen(params: DidOpenNotebookDocumentParams) {
        sendNotification("didOpen", params)
    }

    override suspend fun didChange(params: DidChangeNotebookDocumentParams) {
        sendNotification("didChange", params)
    }

    override suspend fun didSave(params: DidSaveNotebookDocumentParams) {
        sendNotification("didSave", params)
    }

    override suspend fun didClose(params: DidCloseNotebookDocumentParams) {
        sendNotification("didClose", params)
    }
}
