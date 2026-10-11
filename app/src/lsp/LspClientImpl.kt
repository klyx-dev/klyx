package com.klyx.lsp

import com.klyx.editor.EditorStore
import com.klyx.lsp.server.LanguageClient
import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.LSPArray
import com.klyx.lsp.types.LSPObject
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.fold
import com.klyx.runtime.workspace.DocumentId
import com.klyx.runtime.workspace.Workspace
import io.github.lumkit.sweeteditor.EditorDiagnosticSeverity
import io.github.lumkit.sweeteditor.EditorTextEdit
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject

data class LspProgressItem(
    val token: String,
    val title: String,
    val message: String? = null,
    val percentage: UInt? = null,
)

class LspClientImpl(
    private val workspace: Workspace,
    private val editorStore: EditorStore,
) : LanguageClient {

    private val _diagnostics = MutableStateFlow<Map<String, List<Diagnostic>>>(emptyMap())
    val diagnostics: StateFlow<Map<String, List<Diagnostic>>> = _diagnostics.asStateFlow()
    private val diagnosticScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val pendingDiagnosticClears = mutableMapOf<String, kotlinx.coroutines.Job>()

    private val _progressItems = MutableStateFlow<Map<String, LspProgressItem>>(emptyMap())
    val progressItems: StateFlow<Map<String, LspProgressItem>> = _progressItems.asStateFlow()

    var onInlayHintsRefresh: (() -> Unit)? = null
    var onCodeLensesRefresh: (() -> Unit)? = null
    var onDiagnosticsPublished: ((String) -> Unit)? = null
    var onDiagnosticsRefresh: (() -> Unit)? = null

    fun normalizeUri(rawUri: String): String {
        val decoded = if (rawUri.contains('%')) decodePercent(rawUri) else rawUri
        val trimmed = decoded.trimEnd('/')
        return if (trimmed.startsWith("file://")) {
            val path = trimmed.removePrefix("file://")
            if (path.startsWith("/")) path else "/$path"
        } else {
            trimmed
        }
    }

    private fun decodePercent(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 < s.length) {
                val hex = s.substring(i + 1, i + 3)
                val code = hex.toIntOrNull(16)
                if (code != null) {
                    sb.append(code.toChar())
                    i += 3
                    continue
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    private fun documentForUri(uri: String) = workspace.documents.value.values.firstOrNull { document ->
        normalizeUri(LspDocumentUtils.uriOf(document, workspace)) == normalizeUri(uri)
    }

    private fun storedDiagnostics(uri: String) = _diagnostics.value[uri]
        ?: _diagnostics.value.entries.firstOrNull { normalizeUri(it.key) == normalizeUri(uri) }?.value

    private fun rememberDiagnostics(uri: String, diagnostics: List<Diagnostic>) {
        val normalized = normalizeUri(uri)
        _diagnostics.update { current ->
            current.filterKeys { normalizeUri(it) != normalized } + (uri to diagnostics)
        }
    }

    private fun scheduleDiagnosticClear(uri: String) {
        val normalized = normalizeUri(uri)
        pendingDiagnosticClears[normalized]?.cancel()
        pendingDiagnosticClears[normalized] = diagnosticScope.launch {
            delay(800L)
            rememberDiagnostics(uri, emptyList())
            documentForUri(uri)?.let { document ->
                paintDiagnostics(document.name, editorStore.controllersFor(document.id), emptyList())
            }
            pendingDiagnosticClears.remove(normalized)
        }
    }

    private fun cancelDiagnosticClear(uri: String) {
        pendingDiagnosticClears.remove(normalizeUri(uri))?.cancel()
    }

    suspend fun applyDiagnosticsToController(uri: String, controller: SweetEditorController) {
        val rawDiags = storedDiagnostics(uri) ?: return

        paintDiagnostics(docName = uri, targets = listOf(controller), diags = rawDiags)
    }

    private suspend fun paintDiagnostics(
        docName: String,
        targets: Collection<SweetEditorController>,
        diags: List<Diagnostic>,
    ) {
        withContext(Dispatchers.Main) {
            for (target in targets) {
                runCatching {
                    val diagnosticsByLine = LspDiagnosticMapper.map(
                        diags = diags.map { (val range, val severity) ->
                            DiagnosticModel(
                                startLine = range.start.line.toInt(),
                                startCharacter = range.start.character.toInt().coerceAtLeast(0),
                                endLine = range.end.line.toInt(),
                                endCharacter = range.end.character.toInt().coerceAtLeast(0),
                                severity = when (severity) {
                                    DiagnosticSeverity.Error -> EditorDiagnosticSeverity.ERROR
                                    DiagnosticSeverity.Warning -> EditorDiagnosticSeverity.WARNING
                                    DiagnosticSeverity.Information -> EditorDiagnosticSeverity.INFO
                                    DiagnosticSeverity.Hint -> EditorDiagnosticSeverity.HINT
                                    else -> EditorDiagnosticSeverity.ERROR
                                },
                            )
                        },
                        text = target.getDocument().text,
                    )

                    if (diags.isNotEmpty() && diagnosticsByLine.isEmpty()) {
                        println("[LSP Diagnostics] ignored unmappable diagnostics for $docName")
                        return@runCatching
                    }

                    target.clearDiagnostics()
                    if (diagnosticsByLine.isNotEmpty()) {
                        target.setBatchLineDiagnostics(diagnosticsByLine)
                    }
                    target.requestDecorationRefresh()
                }.onFailure {
                    println("[LSP Diagnostics ERROR] paint failed for $docName: ${it.message}")
                }
            }
        }
    }

    override suspend fun publishDiagnostics(params: PublishDiagnosticsParams) {
        val uri = params.uri
        if (params.diagnostics.isEmpty()) {
            scheduleDiagnosticClear(uri)
            return
        }
        cancelDiagnosticClear(uri)
        rememberDiagnostics(uri, params.diagnostics)

        (val id, val name) = documentForUri(uri) ?: return

        val targets = editorStore.controllersFor(id)
        paintDiagnostics(docName = name, targets = targets, diags = params.diagnostics)
        onDiagnosticsPublished?.invoke(uri)
    }

    suspend fun setDiagnostics(
        uri: String,
        diags: List<Diagnostic>,
    ) {
        if (diags.isEmpty()) {
            scheduleDiagnosticClear(uri)
            return
        }
        cancelDiagnosticClear(uri)
        rememberDiagnostics(uri, diags)
        val doc = documentForUri(uri) ?: return
        paintDiagnostics(docName = doc.name, targets = editorStore.controllersFor(doc.id), diags = diags)
        onDiagnosticsPublished?.invoke(uri)
    }

    override suspend fun refreshInlayHints() {
        onInlayHintsRefresh?.invoke()
    }

    override suspend fun refreshCodeLenses() {
        onCodeLensesRefresh?.invoke()
    }

    override suspend fun refreshDiagnostics() {
        onDiagnosticsRefresh?.invoke()
    }

    override suspend fun configuration(params: ConfigurationParams): List<LSPAny> {
        val rustConfig = rustAnalyzerSettings()

        return params.items.map { item ->
            val section = item.section
            when {
                section.isNullOrEmpty() -> buildJsonObject { put("rust-analyzer", rustConfig) }
                section == "rust-analyzer" -> rustConfig
                section == "rust-analyzer.inlayHints" || section == "inlayHints" -> rustConfig["inlayHints"] ?: JsonNull
                section == "rust-analyzer.checkOnSave" || section == "checkOnSave" -> rustConfig["checkOnSave"] ?: JsonNull
                section == "rust-analyzer.check" || section == "check" -> rustConfig["check"] ?: JsonNull
                section == "rust-analyzer.diagnostics" || section == "diagnostics" -> rustConfig["diagnostics"] ?: JsonNull
                section.startsWith("rust-analyzer.") -> {
                    val subKey = section.removePrefix("rust-analyzer.")
                    rustConfig[subKey] ?: JsonNull
                }

                else -> JsonNull
            }
        }
    }

    override suspend fun createProgress(params: WorkDoneProgressCreateParams) {
    }

    override suspend fun notifyProgress(params: ProgressParams) {
        val tokenStr = params.token.fold(left = { it.toString() }, right = { it })
        params.value.fold(
            left = { notif ->
                when (notif) {
                    is WorkDoneProgressBegin -> {
                        _progressItems.update { current ->
                            current + (tokenStr to LspProgressItem(
                                token = tokenStr,
                                title = notif.title,
                                message = notif.message,
                                percentage = notif.percentage,
                            ))
                        }
                    }

                    is WorkDoneProgressReport -> {
                        _progressItems.update { current ->
                            val existing = current[tokenStr]
                            if (existing != null) {
                                current + (tokenStr to existing.copy(
                                    message = notif.message ?: existing.message,
                                    percentage = notif.percentage ?: existing.percentage,
                                ))
                            } else {
                                current + (tokenStr to LspProgressItem(
                                    token = tokenStr,
                                    title = "Working...",
                                    message = notif.message,
                                    percentage = notif.percentage,
                                ))
                            }
                        }
                    }

                    is WorkDoneProgressEnd -> {
                        _progressItems.update { current ->
                            current - tokenStr
                        }
                    }
                }
            },
            right = {
            }
        )
    }

    override suspend fun applyEdit(params: ApplyWorkspaceEditParams): ApplyWorkspaceEditResult {
        val edit = params.edit
        val success = applyWorkspaceEdit(edit)
        return ApplyWorkspaceEditResult(applied = success)
    }

    override suspend fun showMessage(params: ShowMessageParams) {
        println("[LSP MESSAGE] [${params.type}]: ${params.message}")
    }

    override suspend fun showMessageRequest(params: ShowMessageRequestParams): MessageActionItem? {
        println("[LSP MESSAGE REQUEST] [${params.type}]: ${params.message}")
        return null
    }

    override suspend fun logMessage(params: LogMessageParams) {
        if (params.type == MessageType.Error || params.type == MessageType.Warning) {
            println("[LSP LOG] [${params.type}]: ${params.message}")
        }
    }

    override suspend fun telemetryEvent(params: OneOf<LSPObject, LSPArray>) {
    }

    suspend fun applyWorkspaceEdit(edit: WorkspaceEdit): Boolean = withContext(Dispatchers.Main) {
        applyWorkspaceEditInternal(edit)
    }

    private fun applyWorkspaceEditInternal(edit: WorkspaceEdit): Boolean {
        var appliedAll = true

        edit.changes?.forEach { entry ->
            val uri = entry.key
            val textEdits = entry.value
            val doc = workspace.documents.value.values.firstOrNull {
                LspDocumentUtils.uriOf(it, workspace) == uri
            }
            if (doc != null) {
                applyEditsToDocument(doc.id, textEdits)
            } else {
                appliedAll = false
            }
        }

        edit.documentChanges?.forEach { item ->
            item.fold(
                left = { (val textDocument, val edits) ->
                    val uri = textDocument.uri
                    val doc = workspace.documents.value.values.firstOrNull {
                        LspDocumentUtils.uriOf(it, workspace) == uri
                    }
                    if (doc != null) {
                        val edits = edits.mapNotNull { e -> e.fold(left = { it }, right = { null }) }
                        applyEditsToDocument(doc.id, edits)
                    } else {
                        appliedAll = false
                    }
                },
                right = {
                }
            )
        }

        return appliedAll
    }

    private fun applyEditsToDocument(documentId: DocumentId, edits: List<TextEdit>) {
        val sweetEdits = edits.map { edit ->
            EditorTextEdit(
                range = TextRange(
                    start = TextPosition(edit.range.start.line.toInt(), edit.range.start.character.toInt()),
                    end = TextPosition(edit.range.end.line.toInt(), edit.range.end.character.toInt()),
                ),
                newText = edit.newText,
            )
        }

        val _ = editorStore.withDocument(documentId) {
            applyTextEdits(sweetEdits)
        }
    }
}
