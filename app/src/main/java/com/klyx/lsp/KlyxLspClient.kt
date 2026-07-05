package com.klyx.lsp

import android.util.Log
import com.klyx.lsp.server.LanguageClient
import com.klyx.lsp.types.LSPArray
import com.klyx.lsp.types.LSPObject
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.fold
import io.github.rosemoe.sora.compose.CodeEditorState
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class KlyxLspClient(
    private val scope: CoroutineScope
) : LanguageClient {

    private val editors = ConcurrentHashMap<String, CodeEditorState>()

    fun registerEditor(uri: String, state: CodeEditorState) {
        editors[uri] = state
    }

    fun unregisterEditor(uri: String) {
        editors.remove(uri)
    }

    override suspend fun publishDiagnostics(params: PublishDiagnosticsParams) {
        val uri = params.uri
        val editorState = editors[uri] ?: return
        
        val diagnostics = params.diagnostics
        val text = editorState.text
        
        val regions = diagnostics.map { diagnostic ->
            val severity = when (diagnostic.severity) {
                DiagnosticSeverity.Error -> DiagnosticRegion.SEVERITY_ERROR
                DiagnosticSeverity.Warning -> DiagnosticRegion.SEVERITY_WARNING
                DiagnosticSeverity.Information -> DiagnosticRegion.SEVERITY_NONE
                DiagnosticSeverity.Hint -> DiagnosticRegion.SEVERITY_TYPO
                else -> DiagnosticRegion.SEVERITY_ERROR
            }
            
            val startIndex = text.getCharIndex(diagnostic.range.start.line.toInt(), diagnostic.range.start.character.toInt())
            val endIndex = text.getCharIndex(diagnostic.range.end.line.toInt(), diagnostic.range.end.character.toInt())
            
            val message = diagnostic.message.fold({ it }, { it.value })

            DiagnosticRegion(
                startIndex,
                endIndex,
                severity,
                0L,
                DiagnosticDetail(message)
            )
        }

        scope.launch(Dispatchers.Main) {
            val container = editorState.diagnostics ?: DiagnosticsContainer()
            container.reset()
            container.addDiagnostics(regions)
            editorState.diagnostics = container
        }
    }

    override suspend fun showMessage(params: ShowMessageParams) {
        Log.i("LspClient", "Show Message: ${params.message}")
    }

    override suspend fun showMessageRequest(params: ShowMessageRequestParams): MessageActionItem? {
        Log.i("LspClient", "Show Message Request: ${params.message}")
        return null
    }

    override suspend fun logMessage(params: LogMessageParams) {
        Log.i("LspClient", "Log Message: ${params.message}")
    }

    override suspend fun telemetryEvent(params: OneOf<LSPObject, LSPArray>) {
        Log.i("LspClient", "Telemetry Event: $params")
    }
}
