package com.klyx.lsp

import android.util.Log
import com.klyx.lsp.server.LanguageClient
import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.LSPArray
import com.klyx.lsp.types.LSPObject
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.fold
import io.github.rosemoe.sora.compose.CodeEditorState
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.text.Content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import java.util.concurrent.ConcurrentHashMap

internal class DiagnosticsAggregator {
    private val editors = ConcurrentHashMap<String, CodeEditorState>()
    private val bySource = ConcurrentHashMap<String, ConcurrentHashMap<String, List<DiagnosticRegion>>>()

    fun editorFor(uri: String): CodeEditorState? = editors[uri]

    fun registerEditor(uri: String, state: CodeEditorState) {
        editors[uri] = state
    }

    /** Full cleanup when a tab closes entirely. */
    fun removeEditor(uri: String) {
        editors.remove(uri)
        bySource.remove(uri)
    }

    /** Partial cleanup: one server stops contributing (died or unregistered),
     * but the editor may still be served by other providers. */
    fun removeSource(uri: String, serverId: String) {
        bySource[uri]?.remove(serverId)
    }

    suspend fun publish(uri: String, serverId: String, regions: List<DiagnosticRegion>) {
        val editorState = editors[uri] ?: return
        val sourceMap = bySource.getOrPut(uri) { ConcurrentHashMap() }
        sourceMap[serverId] = regions
        val merged = sourceMap.values
            .flatten()
            .distinctBy { Triple(it.startIndex, it.endIndex, it.detail?.briefMessage) }

        withContext(Dispatchers.Main) {
            val container = DiagnosticsContainer()
            container.addDiagnostics(merged)
            editorState.diagnostics = container
        }
    }
}

internal class KlyxLspClient(
    private val scope: CoroutineScope,
    private val serverId: String,
    private val aggregator: DiagnosticsAggregator,
    private val activityStore: LspActivityStore,
    private val serverName: String = serverId,
    private val onRefreshInlayHints: () -> Unit = {}
) : LanguageClient {

    private val registeredUris = ConcurrentHashMap.newKeySet<String>()

    // Once a server is stopped/restarted/crashed the underlying process can keep
    // emitting notifications (rust-analyzer flushes queued window/logMessage traces for
    // a while). Gate every inbound callback so a disposed client never pushes anything
    // into the activity/verbose log again.
    @Volatile
    private var disposed = false

    fun dispose() {
        disposed = true
    }

    fun registerEditor(uri: String, state: CodeEditorState) {
        registeredUris.add(uri)
        aggregator.registerEditor(uri, state)
    }

    fun unregisterEditor(uri: String) {
        registeredUris.remove(uri)
        aggregator.removeSource(uri, serverId)
    }

    /** Called when this server is marked dead so its stale diagnostics don't linger
     * on editors that are still open and served by other providers. */
    fun clearContributedDiagnostics() {
        registeredUris.forEach { uri -> aggregator.removeSource(uri, serverId) }
    }

    override suspend fun publishDiagnostics(params: PublishDiagnosticsParams) {
        if (disposed) return
        publish(params.uri, params.diagnostics)
    }

    private suspend fun publish(uri: String, diagnostics: List<Diagnostic>) {
        val editorState = aggregator.editorFor(uri) ?: return
        val text = editorState.text

        val regions = diagnostics.mapNotNull { diagnostic ->
            runCatching {
                val severity = when (diagnostic.severity) {
                    DiagnosticSeverity.Error -> DiagnosticRegion.SEVERITY_ERROR
                    DiagnosticSeverity.Warning -> DiagnosticRegion.SEVERITY_WARNING
                    DiagnosticSeverity.Information -> DiagnosticRegion.SEVERITY_NONE
                    DiagnosticSeverity.Hint -> DiagnosticRegion.SEVERITY_TYPO
                    else -> DiagnosticRegion.SEVERITY_ERROR
                }

                val startIndex = text.clampedCharIndex(diagnostic.range.start)
                val endIndex = text.clampedCharIndex(diagnostic.range.end).coerceAtLeast(startIndex)
                val message = diagnostic.message.fold({ it }, { it.value })

                DiagnosticRegion(startIndex, endIndex, severity, 0L, DiagnosticDetail(message))
            }.getOrElse {
                Log.w("LspClient", "Skipping malformed diagnostic from $serverId: $it")
                null
            }
        }

        aggregator.publish(uri, serverId, regions)
    }

    private fun Content.clampedCharIndex(position: Position): Int {
        return runCatching {
            val line = position.line.toInt().coerceIn(0, (lineCount - 1).coerceAtLeast(0))
            val column = position.character.toInt().coerceAtMost(getColumnCount(line))
            getCharIndex(line, column)
        }.getOrElse { 0 }
    }

    override suspend fun showMessage(params: ShowMessageParams) {
        if (disposed) return
        activityStore.log(serverId, params.message, params.type.toSeverity())
        //Log.i("LspClient", "Show Message: ${params.message}")
    }

    override suspend fun showMessageRequest(params: ShowMessageRequestParams): MessageActionItem? {
        //Log.i("LspClient", "Show Message Request: ${params.message}")
        return null
    }

    override suspend fun logMessage(params: LogMessageParams) {
        if (disposed) return
        activityStore.log(serverId, params.message, params.type.toSeverity())
        //Log.i("LspClient", "Log Message: ${params.message}")
    }

    override suspend fun notifyProgress(params: ProgressParams) {
        if (disposed) return
        activityStore.progress(serverId, params)
        //Log.i("LspClient", "Progress: $params")
    }

    override suspend fun telemetryEvent(params: OneOf<LSPObject, LSPArray>) {
        Log.i("LspClient", "Telemetry Event: $params")
    }

    override suspend fun registerCapability(params: RegistrationParams) {
        Log.d("LspClient", "Register Capability: $params")
    }

    override suspend fun unregisterCapability(params: UnregistrationParams) {
        Log.d("LspClient", "Unregister Capability: $params")
    }

    override suspend fun workspaceFolders(): List<WorkspaceFolder>? {
        return null
    }

    override suspend fun configuration(params: ConfigurationParams): List<LSPAny> {
        return params.items.map { JsonNull }
    }

    override suspend fun applyEdit(params: ApplyWorkspaceEditParams): ApplyWorkspaceEditResult {
        return ApplyWorkspaceEditResult(applied = false, failureReason = "Not implemented")
    }

    override suspend fun createProgress(params: WorkDoneProgressCreateParams) {
        //activityStore.log(serverId, "Progress started: ${params.token}", LspActivityStore.Severity.Debug)
    }

    override suspend fun showDocument(params: ShowDocumentParams): ShowDocumentResult {
        return ShowDocumentResult(success = false)
    }

    override suspend fun refreshCodeLenses() {
        // No-op
    }

    override suspend fun refreshDiagnostics() {
        // No-op
    }

    override suspend fun refreshFoldingRanges() {
        // No-op
    }

    override suspend fun refreshInlayHints() {
        if (disposed) return
        onRefreshInlayHints()
    }

    override suspend fun refreshInlineValues() {
        // No-op
    }

    override suspend fun refreshSemanticTokens() {
        // No-op
    }
}

private fun MessageType.toSeverity() = when (this) {
    MessageType.Error -> LspActivityStore.Severity.Error
    MessageType.Warning -> LspActivityStore.Severity.Warning
    MessageType.Debug, MessageType.Log -> LspActivityStore.Severity.Debug
    else -> LspActivityStore.Severity.Info
}
