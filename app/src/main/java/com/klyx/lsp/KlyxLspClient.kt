package com.klyx.lsp

import android.util.Log
import com.klyx.lsp.server.LanguageClient
import com.klyx.lsp.server.LanguageServer
import com.klyx.lsp.server.ResponseErrorException
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
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
    private val onRefreshInlayHints: () -> Unit = {},
    private val onRefreshDiagnostics: () -> Unit = {}
) : LanguageClient {

    companion object {
        private const val PULL_RETRY_DELAY_MS = 400L
        private const val PULL_MAX_ATTEMPTS = 10
    }

    private val registeredUris = ConcurrentHashMap.newKeySet<String>()
    private val diagnosticResultIds = ConcurrentHashMap<String, String?>()

    // The single source of truth for open state on this connection: a URI is in
    // this set exactly while didOpen was sent and no didClose has followed.
    // LspManager consults it before every didOpen so a document is never opened
    // twice on one connection — not on editor re-registration, retries, or
    // reconnects. Servers are free to reject the second open.
    private val didOpenedUris = ConcurrentHashMap.newKeySet<String>()

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
        didOpenedUris.remove(uri)
    }

    /** True when [uri] is currently open on this server (didOpen sent, no didClose since). */
    fun isOpen(uri: String): Boolean = didOpenedUris.contains(uri)

    /** Marks [uri] as open on this server. Call right before sending didOpen. */
    fun markOpened(uri: String) {
        didOpenedUris.add(uri)
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

    /**
     * Pulls diagnostics for [uri] via the `textDocument/diagnostic` request.
     * Some servers gate pushed diagnostics behind a completed workspace load
     * but still answer pull requests from analysis done so far.
     */
    suspend fun pullDiagnostics(server: LanguageServer, uri: String) {
        if (disposed) return
        if (aggregator.editorFor(uri) == null) return
        val previous = diagnosticResultIds[uri]
        var attempts = 0
        while (true) {
            attempts++
            val report = try {
                server.textDocument.diagnostic(
                    DocumentDiagnosticParams(
                        textDocument = TextDocumentIdentifier(uri),
                        identifier = null,
                        previousResultId = previous
                    )
                )
            } catch (e: ResponseErrorException) {
                // A ServerCancelled (-32802) response with retriggerRequest=true
                // means "still busy, resend this request once ready", not a real
                // failure. Retry with a short delay instead of giving up.
                val retrigger = e.code == ErrorCodes.ServerCancelled &&
                    ((e.data as? JsonObject)
                        ?.let { runCatching { Json.decodeFromJsonElement<DiagnosticServerCancellationData>(it) }.getOrNull() }
                        ?.retriggerRequest
                        ?: false)
                if (!retrigger) {
                    Log.w("LspClient", "diagnostic pull failed for $uri from $serverId: ${e.message}")
                    return
                }
                if (attempts >= PULL_MAX_ATTEMPTS) {
                    Log.d("LspClient", "diagnostic pull for $uri from $serverId still cancelled after $attempts attempts")
                    return
                }
                Log.d("LspClient", "diagnostics not ready yet for $uri (attempt $attempts/$PULL_MAX_ATTEMPTS), retrying")
                delay(PULL_RETRY_DELAY_MS)
                if (disposed || aggregator.editorFor(uri) == null) return
                continue
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("LspClient", "diagnostic pull failed for $uri from $serverId: ${e.message}")
                return
            }
            val full = report.full ?: return
            diagnosticResultIds[uri] = full.resultId
            publish(uri, full.items)
            Log.d("LspClient", "Pulled ${full.items.size} diagnostics for $uri from $serverId")
            return
        }
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
        params.value.fold({ notification ->
            // A finished work-done progress (indexing, build, ...) means the
            // server's analysis just advanced — ask for fresh diagnostics.
            if (notification is WorkDoneProgressEnd) onRefreshDiagnostics()
        }, {})
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
        if (disposed) return
        onRefreshDiagnostics()
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
