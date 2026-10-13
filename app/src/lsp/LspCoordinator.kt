@file:Suppress("DEPRECATION")

package com.klyx.lsp

import com.klyx.editor.EditorStore
import com.klyx.lsp.types.fold
import com.klyx.runtime.OperatingSystem
import com.klyx.runtime.currentOperatingSystem
import com.klyx.runtime.workspace.Document
import com.klyx.runtime.workspace.DocumentId
import com.klyx.runtime.workspace.ProjectId
import com.klyx.runtime.workspace.Workspace
import com.klyx.ui.editor.CompletionItemRow
import com.klyx.ui.editor.CompletionUiState
import com.klyx.ui.editor.LspCompletionItemUi
import com.klyx.ui.editor.scoreCompletion
import io.github.lumkit.sweeteditor.CodeLensItem
import io.github.lumkit.sweeteditor.CompletionInsertTextFormat
import io.github.lumkit.sweeteditor.DocumentHighlight
import io.github.lumkit.sweeteditor.EditorDocumentHighlightKind
import io.github.lumkit.sweeteditor.EditorTextEdit
import io.github.lumkit.sweeteditor.FoldRegion
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.TextChangeKind
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import okio.FileSystem
import okio.SYSTEM
import org.koin.core.annotation.Single
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class HoverState(
    val line: Int,
    val column: Int,
    val content: String,
)

@Single
class EditorLsp(
    val workspace: Workspace,
    val editorStore: EditorStore,
) : AutoCloseable {

    private class EditorState {
        val disposables = mutableListOf<() -> Unit>()
        var changeJob: Job? = null
        var highlightJob: Job? = null
        var completionJob: Job? = null
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    val client = LspClientImpl(workspace, editorStore)

    private val sessions = mutableMapOf<String, LspSession>()
    private val editorStates = mutableMapOf<DocumentId, EditorState>()
    private val inlayHintJobs = mutableMapOf<String, Job>()
    private val inlayHintGeneration = mutableMapOf<String, Int>()
    private val documentCodeLenses = mutableMapOf<DocumentId, List<CodeLens>>()

    private val _activeHover = MutableStateFlow<HoverState?>(null)
    val activeHover: StateFlow<HoverState?> = _activeHover.asStateFlow()

    fun dismissHover() {
        _activeHover.value = null
    }

    private val _activeCompletion = MutableStateFlow<CompletionUiState?>(null)
    val activeCompletion: StateFlow<CompletionUiState?> = _activeCompletion.asStateFlow()

    fun dismissCompletionPopup() {
        editorStates.values.forEach { it.completionJob?.cancel() }
        _activeCompletion.value = null
    }

    private val _activeSessions = MutableStateFlow<Map<String, LspSessionState>>(emptyMap())
    val activeSessions: StateFlow<Map<String, LspSessionState>> = _activeSessions.asStateFlow()

    private val _lspErrors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val lspErrors: SharedFlow<String> = _lspErrors.asSharedFlow()

    private val _termuxAvailability = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val termuxAvailability: StateFlow<Map<String, Boolean>> = _termuxAvailability.asStateFlow()

    init {
        observeWorkspaceProjects()
        observeInlayHintsSetting()
        observeCustomServers()
        observeServerAvailability()
        setupClientCallbacks()
    }

    private fun setupClientCallbacks() {
        client.onInlayHintsRefresh = {
            refreshActiveDecorations(inlay = true)
        }
        client.onCodeLensesRefresh = {
            refreshActiveDecorations(codeLens = true)
        }
        client.onDiagnosticsRefresh = {
            refreshActiveDiagnostics()
        }
        client.onDiagnosticsPublished = { uri ->
            refreshActiveDecorations(inlay = true, targetUri = uri, inlayDebounce = 200.milliseconds)
        }
    }

    private fun refreshActiveDiagnostics() {
        scope.launch {
            val activeDocId = editorStore.activeDocumentId.value ?: return@launch
            val document = workspace.document(activeDocId) ?: return@launch
            val session = sessionForDocument(document) ?: return@launch
            refreshDiagnostics(session, LspDocumentUtils.uriOf(document, workspace))
        }
    }

    private suspend fun refreshDiagnostics(
        session: LspSession,
        uri: String,
    ) {
        val pulled = session.pullDiagnostics(uri) ?: return
        client.setDiagnostics(uri, pulled)
    }

    private fun refreshActiveDecorations(
        inlay: Boolean = false,
        codeLens: Boolean = false,
        targetUri: String? = null,
        inlayDebounce: Duration = 0.milliseconds,
    ) {
        scope.launch {
            val activeDocId = editorStore.activeDocumentId.value ?: return@launch
            val doc = workspace.document(activeDocId) ?: return@launch
            val session = sessionForDocument(doc) ?: return@launch
            val docUri = LspDocumentUtils.uriOf(doc, workspace)
            if (targetUri != null && client.normalizeUri(targetUri) != client.normalizeUri(docUri)) {
                return@launch
            }
            val controllers = editorStore.controllersFor(activeDocId)
            val primary = controllers.firstOrNull() ?: return@launch

            if (inlay) {
                scheduleInlayHintRefresh(session, docUri, primary, inlayDebounce)
            }
            if (codeLens) {
                controllers.forEach { controller ->
                    refreshCodeLens(session, docUri, controller)
                }
            }
        }
    }

    fun sessionFor(languageId: String): LspSession? = sessions[languageId]

    fun sessionForDocument(document: Document): LspSession? {
        val langId = LspDocumentUtils.languageIdOf(document)
        return sessions[langId]
    }

    suspend fun ensureSession(document: Document): LspSession? {
        val langId = LspDocumentUtils.languageIdOf(document)
        val existing = sessions[langId]
        if (existing != null && existing.state.value == LspSessionState.Running) {
            return existing
        }

        val config = LspServerRegistry.findByLanguageId(langId)
            ?: LspServerRegistry.findByExtension(document.name.substringAfterLast('.', ""))
            ?: run {
                _lspErrors.tryEmit("No language server is configured for ${langId}.")
                return null
            }
        val session = existing ?: LspSession(config, client, scope).also { newSession ->
            sessions[langId] = newSession
            scope.launch {
                newSession.state.collect {
                    updateActiveSessions()
                }
            }
        }

        val activeProj = workspace.activeProjectId.value?.let { workspace.projects.value[it] }
        val docPath = LspDocumentUtils.pathOf(document, workspace)
        val projectRoot = activeProj?.root ?: findProjectRoot(docPath)
        val rootUri = projectRoot?.let { "file://$it" }
        val folders = if (activeProj != null) {
            workspace.projects.value.values.map {
                WorkspaceFolder(uri = "file://${it.root}", name = it.root.name)
            }
        } else if (projectRoot != null) {
            listOf(WorkspaceFolder(uri = "file://$projectRoot", name = projectRoot.name))
        } else {
            emptyList()
        }

        println("[LSP] Starting session for ${config.languageId} with root: $rootUri")
        val started = session.start(rootUri, folders)
        updateActiveSessions()
        if (!started) {
            val base = session.startError ?: "Unknown error"
            val hint = when {
                config.command == "vtsls" -> "$base\nInstall the language server in Termux: pkg install -y nodejs && npm i -g @vtsls/language-server"
                else -> base
            }
            _lspErrors.tryEmit("${config.languageId} language server failed to start: $hint")
        }
        return if (started) session else null
    }

    suspend fun restartSession(languageId: String): Boolean {
        val session = sessions.remove(languageId) ?: return false
        session.close()
        updateActiveSessions()
        val activeDocId = editorStore.activeDocumentId.value
        val doc = activeDocId?.let { workspace.document(it) }
        return if (doc != null && LspDocumentUtils.languageIdOf(doc) == languageId) {
            restartLanguageServerForDocument(doc)
        } else {
            false
        }
    }

    suspend fun restartLanguageServerForDocument(document: Document): Boolean {
        val langId = LspDocumentUtils.languageIdOf(document)
        val existing = sessions.remove(langId)
        existing?.close()
        updateActiveSessions()
        val newSession = ensureSession(document)
        val controller = editorStore.controllersFor(document.id).firstOrNull()
        if (newSession != null && controller != null) {
            val uri = LspDocumentUtils.uriOf(document, workspace)
            val text = controller.getDocument().text.ifEmpty { workspace.readText(document.id) }
            newSession.didOpen(uri, langId, text)
            refreshFolding(newSession, uri, controller)
            refreshCodeLens(newSession, uri, controller)
            scheduleInlayHintRefresh(newSession, uri, controller)
        }
        return newSession != null
    }

    private fun findProjectRoot(filePath: okio.Path?): okio.Path? {
        var current = filePath?.parent ?: return null
        val markers = setOf("Cargo.toml", "go.mod", "package.json", "pom.xml", "build.gradle.kts", ".git")
        while (true) {
            for (marker in markers) {
                if (FileSystem.SYSTEM.exists(current / marker)) {
                    return current
                }
            }
            val parent = current.parent ?: break
            current = parent
        }
        return null
    }

    fun bindEditor(document: Document, controller: SweetEditorController) {
        val state = editorStates.getOrPut(document.id) { EditorState() }
        val unbindList = state.disposables
        unbindList.forEach { it.invoke() }
        unbindList.clear()

        val uri = LspDocumentUtils.uriOf(document, workspace)
        val langId = LspDocumentUtils.languageIdOf(document)

        controller.setCompletionItemRenderer { item, selected, theme ->
            CompletionItemRow(
                item = item,
                selected = selected,
                theme = theme,
            )
        }

        val unsubText = controller.onTextChanged { (val changes, val kind) ->
            state.changeJob?.cancel()
            state.changeJob = scope.launch {
                delay(500.milliseconds)
                val latestText = controller.getDocument().text
                val liveSession = sessionForDocument(document)
                if (workspace.autoSave.value) {
                    try {
                        workspace.writeText(document.id, latestText)
                        notifyDocumentSaved(document, latestText)
                    } catch (e: Exception) {
                        if (e is CancellationException) {
                            ensureActive()
                        }
                        println("[AutoSave ERROR] Failed to save ${document.name}: ${e.message}")
                    }
                }
                if (liveSession != null) {
                    liveSession.requestDiagnosticsAfterChange(uri, latestText)
                    refreshDiagnostics(liveSession, uri)
                    if (liveSession.config.languageId == "rust") {
                        scope.launch {
                            delay(750.milliseconds)
                            refreshDiagnostics(liveSession, uri)
                            delay(1000.milliseconds)
                            refreshDiagnostics(liveSession, uri)
                        }
                    }
                    refreshFolding(liveSession, uri, controller)
                    refreshCodeLens(liveSession, uri, controller)
                    scheduleInlayHintRefresh(liveSession, uri, controller)
                }
            }

            if (sessionForDocument(document) != null) {
                when (kind) {
                    TextChangeKind.INSERTION, TextChangeKind.REPLACEMENT -> {
                        val anyNewlineOrSpace = changes.any { it.newText.contains('\n') || it.newText == " " }
                        if (anyNewlineOrSpace) {
                            dismissCompletionPopup()
                        } else {
                            val shouldTrigger = changes.any { change ->
                                change.newText.any { it.isLetterOrDigit() || it == '_' || it == '.' || it == ':' || it == '>' || it == '$' }
                            }
                            if (shouldTrigger) {
                                triggerCompletionPopup(document, controller)
                            }
                        }
                    }

                    TextChangeKind.DELETION -> {
                        val word = controller.getWordAtCursor()
                        if (word.isNotEmpty() && word.any { it.isLetterOrDigit() || it == '_' }) {
                            triggerCompletionPopup(document, controller)
                        } else {
                            dismissCompletionPopup()
                        }
                    }

                    TextChangeKind.UNDO, TextChangeKind.REDO -> {
                        dismissCompletionPopup()
                    }

                    else -> Unit
                }
            }
        }
        unbindList.add(unsubText)

        scope.launch {
            val session = ensureSession(document) ?: return@launch

            controller.whenReady {
                scope.launch {
                    val docText = controller.getDocument().text
                    val text = docText.ifEmpty { workspace.readText(document.id) }
                    session.didOpen(uri, langId, text)
                    client.applyDiagnosticsToController(uri, controller)
                    refreshDiagnostics(session, uri)
                    refreshFolding(session, uri, controller)
                    refreshCodeLens(session, uri, controller)
                    scheduleInlayHintRefresh(session, uri, controller)

                    delay(1200.milliseconds)
                    refreshDiagnostics(session, uri)
                    scheduleInlayHintRefresh(session, uri, controller)
                    refreshCodeLens(session, uri, controller)
                }
            }

            val unsubLongPress = controller.onLongPress { event ->
                scope.launch {
                    val _ = requestHover(document, event.cursorPosition.line, event.cursorPosition.column)
                }
            }
            unbindList.add(unsubLongPress)

            val unsubCursor = controller.onCursorChanged { event ->
                if (_activeHover.value?.let { it.line != event.cursorPosition.line } == true) {
                    dismissHover()
                }
                if (_activeCompletion.value?.let { it.anchor == null } == true) {
                    dismissCompletionPopup()
                }
                state.highlightJob?.cancel()
                state.highlightJob = scope.launch {
                    delay(120.milliseconds)
                    refreshDocumentHighlights(
                        session,
                        uri,
                        event.cursorPosition.line,
                        event.cursorPosition.column,
                        controller,
                    )
                }
            }
            unbindList.add(unsubCursor)

            val unsubCodeLens = controller.onCodeLensClick { event ->
                val lenses = documentCodeLenses[document.id] ?: return@onCodeLensClick
                val lens = lenses.find { it.range.start.line.toInt() == event.line }
                (val command, val arguments) = lens?.command ?: return@onCodeLensClick
                scope.launch {
                    val args = (arguments as? JsonArray)?.toList()
                    val _ = session.executeCommand(command, args)
                }
            }
            unbindList.add(unsubCodeLens)
        }
    }

    fun unbindEditor(document: Document) {
        val state = editorStates.remove(document.id)
        state?.disposables?.forEach { it.invoke() }
        state?.changeJob?.cancel()
        state?.highlightJob?.cancel()
        state?.completionJob?.cancel()
        val docUri = LspDocumentUtils.uriOf(document, workspace)
        inlayHintJobs.remove(docUri)?.cancel()
        inlayHintGeneration.remove(docUri)
        if (_activeCompletion.value?.documentId == document.id) {
            _activeCompletion.value = null
        }
        documentCodeLenses.remove(document.id)

        scope.launch {
            val controller = editorStore.controllersFor(document.id).firstOrNull()
            if (controller != null) {
                val text = controller.getDocument().text
                try {
                    workspace.writeText(document.id, text)
                    notifyDocumentSaved(document, text)
                } catch (_: Exception) {
                    ensureActive()
                }
            }
            val session = sessionForDocument(document) ?: return@launch
            val uri = LspDocumentUtils.uriOf(document, workspace)
            session.didClose(uri)
        }
    }

    suspend fun notifyDocumentSaved(document: Document, text: String? = null) {
        val session = sessionForDocument(document) ?: return
        val uri = LspDocumentUtils.uriOf(document, workspace)
        val docText = text ?: workspace.readText(document.id)
        session.didSave(uri, docText)
        refreshDiagnostics(session, uri)
        val _ = editorStore.withDocument(document.id) {
            val controller = this@withDocument
            scope.launch {
                refreshFolding(session, uri, controller)
                refreshCodeLens(session, uri, controller)
            }
        }
    }

    suspend fun saveDocument(document: Document) {
        val controller = editorStore.controllersFor(document.id).firstOrNull()
        val text = controller?.getDocument()?.text ?: workspace.readText(document.id)
        try {
            workspace.writeText(document.id, text)
            notifyDocumentSaved(document, text)
        } catch (e: Exception) {
            if (e is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
            println("[Save ERROR] Failed to save ${document.name}: ${e.message}")
        }
    }

    suspend fun saveDocument(documentId: DocumentId) {
        val doc = workspace.document(documentId) ?: return
        saveDocument(doc)
    }

    suspend fun saveActiveDocument() {
        val activeDocId = editorStore.activeDocumentId.value ?: return
        saveDocument(activeDocId)
    }

    suspend fun saveAllDocuments() {
        workspace.documents.value.values.forEach { doc ->
            saveDocument(doc)
        }
    }

    suspend fun formatDocument(document: Document, controller: SweetEditorController): Boolean {
        val session = sessionForDocument(document) ?: return false
        val uri = LspDocumentUtils.uriOf(document, workspace)
        val edits = session.formatting(uri) ?: return false

        val sweetEdits = edits.map { edit ->
            EditorTextEdit(
                range = TextRange(
                    start = TextPosition(edit.range.start.line.toInt(), edit.range.start.character.toInt()),
                    end = TextPosition(edit.range.end.line.toInt(), edit.range.end.character.toInt()),
                ),
                newText = edit.newText,
            )
        }
        withContext(Dispatchers.Main) {
            controller.applyTextEdits(sweetEdits)
        }
        return true
    }

    private suspend fun refreshFolding(session: LspSession, uri: String, controller: SweetEditorController) {
        val ranges = session.foldingRange(uri)
        if (ranges.isEmpty()) {
            withContext(Dispatchers.Main) {
                controller.setFoldRegions(emptyList())
                controller.requestDecorationRefresh()
            }
            return
        }

        val validRanges = ranges.filter { (startLine, endLine) -> startLine < endLine }
        val byStartLine = validRanges.groupBy { it.startLine }
        val foldRegions = byStartLine.values.mapNotNull { group ->
            (val startLine, val endLine) = group.maxByOrNull { it.endLine } ?: return@mapNotNull null
            FoldRegion(startLine = startLine.toInt(), endLine = endLine.toInt(), collapsed = false)
        }
        withContext(Dispatchers.Main) {
            controller.setFoldRegions(foldRegions)
            controller.requestDecorationRefresh()
        }
    }

    private suspend fun refreshCodeLens(session: LspSession, uri: String, controller: SweetEditorController) {
        val lenses = session.codeLens(uri)
        if (lenses.isEmpty()) {
            withContext(Dispatchers.Main) {
                controller.clearCodeLens()
                controller.requestDecorationRefresh()
            }
            return
        }

        val resolvedLenses = lenses.map { lens ->
            if (lens.command == null) {
                session.resolveCodeLens(lens)
            } else {
                lens
            }
        }

        val doc = workspace.documents.value.values.firstOrNull {
            LspDocumentUtils.uriOf(it, workspace) == uri
        }
        if (doc != null) {
            documentCodeLenses[doc.id] = resolvedLenses
        }

        val itemsByLine = mutableMapOf<Int, MutableList<CodeLensItem>>()
        for ((val index, val lens = value) in resolvedLenses.withIndex()) {
            val title = lens.command?.title
            if (title.isNullOrBlank()) continue
            val line = lens.range.start.line.toInt()
            val col = lens.range.start.character.toInt()
            itemsByLine.getOrPut(line) { mutableListOf() }
                .add(CodeLensItem(column = col, commandId = index, text = title))
        }

        withContext(Dispatchers.Main) {
            controller.clearCodeLens()
            if (itemsByLine.isNotEmpty()) {
                controller.setBatchLineCodeLens(itemsByLine)
            }
            controller.requestDecorationRefresh()
        }
    }

    private suspend fun refreshDocumentHighlights(
        session: LspSession,
        uri: String,
        line: Int,
        column: Int,
        controller: SweetEditorController,
    ) {
        val highlights = session.documentHighlight(uri, line, column)
        if (highlights.isEmpty()) {
            withContext(Dispatchers.Main) {
                controller.clearDocumentHighlights()
                controller.requestDecorationRefresh()
            }
            return
        }

        val highlightsByLine = mutableMapOf<Int, MutableList<DocumentHighlight>>()
        for ((val range, val kind) in highlights) {
            val hLine = range.start.line.toInt()
            val hCol = range.start.character.toInt()
            val hEndCol = range.end.character.toInt()
            val len = if (range.end.line.toInt() == hLine) (hEndCol - hCol).coerceAtLeast(1) else 1
            val kind = when (kind) {
                DocumentHighlightKind.Read -> EditorDocumentHighlightKind.READ
                DocumentHighlightKind.Write -> EditorDocumentHighlightKind.WRITE
                else -> EditorDocumentHighlightKind.TEXT
            }
            highlightsByLine.getOrPut(hLine) { mutableListOf() }
                .add(DocumentHighlight(column = hCol, length = len, kind = kind))
        }
        withContext(Dispatchers.Main) {
            controller.setBatchLineDocumentHighlights(highlightsByLine)
            controller.requestDecorationRefresh()
        }
    }


    private fun scheduleInlayHintRefresh(
        session: LspSession,
        uri: String,
        controller: SweetEditorController,
        debounce: Duration = 0.milliseconds,
    ) {
        if (!workspace.showInlayHints.value) {
            inlayHintJobs.remove(uri)?.cancel()
            inlayHintGeneration.remove(uri)
            scope.launch {
                withContext(Dispatchers.Main) {
                    controller.clearInlayHints()
                    controller.requestDecorationRefresh()
                }
            }
            return
        }
        inlayHintJobs[uri]?.cancel()
        val generation = (inlayHintGeneration[uri] ?: 0) + 1
        inlayHintGeneration[uri] = generation
        inlayHintJobs[uri] = scope.launch {
            if (debounce > 0.milliseconds) {
                delay(debounce)
            }
            refreshInlayHints(session, uri, controller, generation)
        }
    }

    private suspend fun refreshInlayHints(
        session: LspSession,
        uri: String,
        controller: SweetEditorController,
        generation: Int,
    ) {
        if (!session.supportsInlayHints()) return
        val fetchedText = withContext(Dispatchers.Main) { controller.getDocument().text }
        session.syncTextIfNeeded(uri, fetchedText)

        val lines = fetchedText.lines()
        val lastLineIndex = (lines.size - 1).coerceAtLeast(0)
        val lastCol = lines.getOrNull(lastLineIndex)?.length ?: 0
        val range = Range(Position(0u, 0u), Position(lastLineIndex.toUInt(), lastCol.toUInt()))
        val hints = session.inlayHint(uri, range) ?: return

        if (inlayHintGeneration[uri] != generation) return

        val currentText = withContext(Dispatchers.Main) { controller.getDocument().text }
        if (currentText != fetchedText) return
        if (!workspace.showInlayHints.value) return

        val currentLines = currentText.lines()
        val hintsByLine = mutableMapOf<Int, MutableList<io.github.lumkit.sweeteditor.InlayHint>>()
        if (hints.isNotEmpty()) {
            for ((val position, val label, val paddingLeft, val paddingRight) in hints) {
                val hLine = position.line.toInt()
                if (hLine !in currentLines.indices) continue
                val lineLen = currentLines[hLine].length
                val hCol = position.character.toInt().coerceIn(0, lineLen)
                val labelText = label.fold(
                    left = { it },
                    right = { parts -> parts.joinToString("") { part -> part.value } }
                )
                if (labelText.isEmpty()) continue
                val paddedText =
                    (if (paddingLeft == true) " " else "") + labelText + (if (paddingRight == true) " " else "")
                hintsByLine.getOrPut(hLine) { mutableListOf() }
                    .add(
                        io.github.lumkit.sweeteditor.InlayHint(
                            type = io.github.lumkit.sweeteditor.EditorInlayType.TEXT,
                            column = hCol,
                            text = paddedText,
                        )
                    )
            }
            hintsByLine.values.forEach { list -> list.sortBy { it.column } }
        }

        if (inlayHintGeneration[uri] != generation) return
        val docId = workspace.documents.value.values.firstOrNull {
            LspDocumentUtils.uriOf(it, workspace) == uri
        }?.id
        val targets = docId?.let { editorStore.controllersFor(it) } ?: setOf(controller)
        withContext(Dispatchers.Main) {
            targets.forEach { target ->
                target.clearInlayHints()
                if (hintsByLine.isNotEmpty()) {
                    target.setBatchLineInlayHints(hintsByLine)
                }
                target.requestDecorationRefresh()
            }
        }
    }

    suspend fun requestHover(document: Document, line: Int, column: Int): HoverState? {
        val session = sessionForDocument(document) ?: return null
        val uri = LspDocumentUtils.uriOf(document, workspace)
        val hover = session.hover(uri, line, column) ?: return null
        val text = hover.contents.fold(
            firstFn = { marked ->
                marked.fold(left = { it }, right = { (language, value) -> "```$language\n$value\n```" })
            },
            secondFn = { list ->
                list.joinToString("\n\n") { marked ->
                    marked.fold(left = { it }, right = { (language, value) -> "```$language\n$value\n```" })
                }
            },
            thirdFn = { it.value }
        )
        if (text.isBlank()) return null
        val state = HoverState(line, column, text.trim())
        _activeHover.value = state
        return state
    }

    suspend fun getCodeActions(
        document: Document,
        range: Range,
        diagnostics: List<Diagnostic> = emptyList(),
    ): List<CodeAction> {
        val session = sessionForDocument(document) ?: return emptyList()
        val uri = LspDocumentUtils.uriOf(document, workspace)
        return session.codeAction(uri, range, diagnostics) ?: emptyList()
    }

    suspend fun applyCodeAction(action: CodeAction) {
        action.edit?.let { edit ->
            client.applyWorkspaceEdit(edit)
        }
        action.command?.let { (val command, val arguments) ->
            val langId = editorStore.activeDocumentId.value?.let { workspace.document(it) }
                ?.let { LspDocumentUtils.languageIdOf(it) }
            val session = langId?.let { sessionFor(it) }
            val args = (arguments as? JsonArray)?.toList()
            val _ = session?.executeCommand(command, args)
        }
        editorStore.activeDocumentId.value?.let { docId ->
            workspace.document(docId)?.let { saveDocument(it) }
        }
    }

    fun triggerCompletionPopup(document: Document, controller: SweetEditorController) {
        val session = sessionForDocument(document) ?: return
        val uri = LspDocumentUtils.uriOf(document, workspace)
        val state = editorStates.getOrPut(document.id) { EditorState() }
        state.completionJob?.cancel()
        state.completionJob = scope.launch {
            delay(70.milliseconds)
            (val line, val column) = withContext(Dispatchers.Main) { controller.getCursorPosition() } ?: return@launch
            val fullText = withContext(Dispatchers.Main) { controller.getDocument().text }
            session.syncTextIfNeeded(uri, fullText)

            val rawItems = session.completion(uri, line, column)
            if (rawItems.isEmpty()) {
                _activeCompletion.value = null
                return@launch
            }

            val lines = fullText.lines()
            val lineStr = lines.getOrNull(line) ?: ""
            val col = column.coerceIn(0, lineStr.length)
            var prefixStart = col
            while (prefixStart > 0 && (lineStr[prefixStart - 1].isLetterOrDigit() || lineStr[prefixStart - 1] == '_')) {
                prefixStart--
            }
            val prefix = lineStr.substring(prefixStart, col)

            val filteredItems = if (prefix.isEmpty()) {
                rawItems
            } else {
                rawItems.filter { (val label, val filterText) ->
                    label.contains(prefix, ignoreCase = true) ||
                            filterText?.contains(prefix, ignoreCase = true) == true
                }
            }
            if (filteredItems.isEmpty()) {
                _activeCompletion.value = null
                return@launch
            }

            val uiItems = filteredItems.map { item ->
                val sweetItem = buildSweetCompletionItem(item, line, prefixStart, col, prefix)
                val docText = item.documentation?.fold(
                    left = { it },
                    right = { it.value }
                )
                val isDeprecated = item.deprecated == true ||
                        item.tags?.contains(CompletionItemTag.Deprecated) == true
                val isSnippet = item.insertTextFormat == InsertTextFormat.Snippet

                LspCompletionItemUi(
                    label = item.label,
                    detail = item.detail,
                    labelDetail = item.labelDetails?.detail,
                    labelDescription = item.labelDetails?.description,
                    documentation = docText,
                    kind = item.kind,
                    isDeprecated = isDeprecated,
                    isSnippet = isSnippet,
                    rawItem = item,
                    sweetItem = sweetItem,
                    line = line,
                    prefixStart = item.textEdit?.let { edit ->
                        edit.fold(
                            left = { it.range.start.character.toInt() },
                            right = { it.replace.start.character.toInt() },
                        )
                    } ?: prefixStart,
                    prefixEnd = col,
                )
            }.sortedWith(
                compareBy<LspCompletionItemUi> { item ->
                    scoreCompletion(item.rawItem, prefix)
                }.thenBy { (val label, val rawItem) ->
                    rawItem.sortText ?: label
                }.thenBy { item ->
                    item.label
                }
            )

            val anchor = withContext(Dispatchers.Main) { controller.getCursorRect() }
            _activeCompletion.value = CompletionUiState(
                documentId = document.id,
                items = uiItems,
                selectedIndex = 0,
                anchor = anchor,
            )
        }
    }

    fun applyCompletion(item: LspCompletionItemUi, controller: SweetEditorController) {
        dismissCompletionPopup()
        applyCompletionReplacingPrefix(item, controller)
    }


    private fun applyCompletionReplacingPrefix(
        item: LspCompletionItemUi,
        controller: SweetEditorController,
    ) {
        val prefixStart = item.prefixStart
        val prefixEnd = item.prefixEnd
        if (prefixStart == null || prefixEnd == null || prefixStart == prefixEnd) {
            controller.applyCompletionItem(item.sweetItem)
            return
        }

        val rawInsert = item.sweetItem.insertText ?: item.label
        (val finalText = first, val cursorOffset = second) = expandSnippetPlaceholders(rawInsert)
        val replaceRange = TextRange(
            start = TextPosition(item.line, prefixStart),
            end = TextPosition(item.line, prefixEnd),
        )
        val extra = item.sweetItem.additionalTextEdits.filterNot { (val range, val newText) ->
            range == replaceRange && newText.isEmpty()
        }
        controller.applyTextEdits(
            listOf(EditorTextEdit(range = replaceRange, newText = finalText)) + extra
        )
        controller.setCursorPosition(item.line, prefixStart + cursorOffset)
    }


    private fun expandSnippetPlaceholders(template: String): Pair<String, Int> {
        var cursor = template.indexOf($$"$0")
        var text = template.replace($$"$0", "")
        if (cursor < 0) cursor = Int.MIN_VALUE
        text = snippetDefaultPattern.replace(text) { it.groupValues[2] }
        text = snippetTabStopPattern.replace(text, "")
        if (cursor == Int.MIN_VALUE) cursor = text.length
        return text to cursor.coerceIn(0, text.length)
    }

    private val snippetDefaultPattern = Regex("""\$\{(\d+):([^}]*)\}""")
    private val snippetTabStopPattern = Regex("""\$(\d+)""")

    suspend fun resolveCompletionDoc(document: Document, item: LspCompletionItemUi): LspCompletionItemUi {
        val session = sessionForDocument(document) ?: return item
        if (!item.documentation.isNullOrBlank()) return item
        val resolved = session.resolveCompletionItem(item.rawItem)
        val docText = resolved.documentation?.fold(
            left = { it },
            right = { it.value }
        ) ?: resolved.detail
        return item.copy(
            documentation = docText,
            detail = resolved.detail ?: item.detail,
            labelDetail = resolved.labelDetails?.detail ?: item.labelDetail,
            labelDescription = resolved.labelDetails?.description ?: item.labelDescription,
            rawItem = resolved,
        )
    }

    private fun buildSweetCompletionItem(
        item: CompletionItem,
        line: Int,
        prefixStart: Int,
        prefixEnd: Int,
        prefix: String,
    ): io.github.lumkit.sweeteditor.CompletionItem {
        val isSnippet = item.insertTextFormat == InsertTextFormat.Snippet

        val baseAdditionalEdits = item.additionalTextEdits?.map { edit ->
            EditorTextEdit(
                range = TextRange(
                    start = TextPosition(edit.range.start.line.toInt(), edit.range.start.character.toInt()),
                    end = TextPosition(edit.range.end.line.toInt(), edit.range.end.character.toInt()),
                ),
                newText = edit.newText,
            )
        } ?: emptyList()

        val rawTextEdit = item.textEdit?.fold(
            left = { it },
            right = { (newText, replace) -> TextEdit(replace, newText) }
        )

        val format: CompletionInsertTextFormat
        val insertText: String
        val textEdit: EditorTextEdit?
        val additionalEdits: List<EditorTextEdit>

        if (isSnippet) {
            format = CompletionInsertTextFormat.SNIPPET
            insertText = rawTextEdit?.newText ?: item.insertText ?: item.label
            textEdit = null

            val prefixDeleteEdit = if (rawTextEdit != null) {
                val start =
                    TextPosition(rawTextEdit.range.start.line.toInt(), rawTextEdit.range.start.character.toInt())
                val end = TextPosition(rawTextEdit.range.end.line.toInt(), rawTextEdit.range.end.character.toInt())
                if (start != end) {
                    listOf(EditorTextEdit(range = TextRange(start = start, end = end), newText = ""))
                } else {
                    emptyList()
                }
            } else if (prefixStart < prefixEnd) {
                listOf(
                    EditorTextEdit(
                        range = TextRange(
                            start = TextPosition(line, prefixStart),
                            end = TextPosition(line, prefixEnd),
                        ),
                        newText = "",
                    )
                )
            } else {
                emptyList()
            }

            additionalEdits = prefixDeleteEdit + baseAdditionalEdits
        } else {
            format = CompletionInsertTextFormat.PLAIN_TEXT
            insertText = item.insertText ?: item.label
            textEdit = rawTextEdit?.let { edit ->
                EditorTextEdit(
                    range = TextRange(
                        start = TextPosition(edit.range.start.line.toInt(), edit.range.start.character.toInt()),
                        end = TextPosition(edit.range.end.line.toInt(), edit.range.end.character.toInt()),
                    ),
                    newText = edit.newText,
                )
            }
            additionalEdits = baseAdditionalEdits
        }

        val sweetKind = when (item.kind) {
            CompletionItemKind.Keyword -> io.github.lumkit.sweeteditor.CompletionItemKind.KEYWORD
            CompletionItemKind.Function,
            CompletionItemKind.Method,
            CompletionItemKind.Constructor -> io.github.lumkit.sweeteditor.CompletionItemKind.FUNCTION

            CompletionItemKind.Variable,
            CompletionItemKind.Field,
            CompletionItemKind.Constant,
            CompletionItemKind.Value -> io.github.lumkit.sweeteditor.CompletionItemKind.VARIABLE

            CompletionItemKind.Class,
            CompletionItemKind.Enum,
            CompletionItemKind.Struct -> io.github.lumkit.sweeteditor.CompletionItemKind.CLASS

            CompletionItemKind.Interface -> io.github.lumkit.sweeteditor.CompletionItemKind.INTERFACE
            CompletionItemKind.Module -> io.github.lumkit.sweeteditor.CompletionItemKind.MODULE
            CompletionItemKind.Property -> io.github.lumkit.sweeteditor.CompletionItemKind.PROPERTY
            CompletionItemKind.Snippet -> io.github.lumkit.sweeteditor.CompletionItemKind.SNIPPET
            else -> io.github.lumkit.sweeteditor.CompletionItemKind.TEXT
        }

        val serverSortKey = item.sortText ?: item.label
        val score = scoreCompletion(item, prefix)
        val compositeSortKey = "${score}_$serverSortKey"

        return io.github.lumkit.sweeteditor.CompletionItem(
            label = item.label,
            detail = item.detail ?: "",
            insertText = insertText,
            insertTextFormat = format,
            textEdit = textEdit,
            additionalTextEdits = additionalEdits,
            filterText = item.filterText,
            sortKey = compositeSortKey,
            kind = sweetKind,
        )
    }

    private fun observeWorkspaceProjects() {
        var previousProjectIds = emptySet<ProjectId>()
        scope.launch {
            workspace.projects.collect { projects ->
                val currentIds = projects.keys
                val added = (currentIds - previousProjectIds).mapNotNull { projects[it] }
                val removed = (previousProjectIds - currentIds)

                if (previousProjectIds.isNotEmpty() && (added.isNotEmpty() || removed.isNotEmpty())) {
                    val addedFolders = added.map { WorkspaceFolder(uri = "file://${it.root}", name = it.root.name) }
                    val removedFolders = removed.map { WorkspaceFolder(uri = "project://$it", name = it.value) }

                    sessions.values.forEach { session ->
                        session.didChangeWorkspaceFolders(added = addedFolders, removed = removedFolders)
                    }
                }
                previousProjectIds = currentIds
            }
        }
    }

    private fun observeInlayHintsSetting() {
        scope.launch {
            workspace.showInlayHints.collect { show ->
                if (!show) {
                    inlayHintJobs.values.forEach { it.cancel() }
                    inlayHintJobs.clear()
                    inlayHintGeneration.clear()
                    withContext(Dispatchers.Main) {
                        editorStore.forEach { _, controller ->
                            controller.clearInlayHints()
                            controller.requestDecorationRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun observeCustomServers() {
        scope.launch {
            workspace.customLspServers.collect { servers ->
                LspServerRegistry.setCustomServers(servers.map { it.toConfig() })
            }
        }
    }

    private fun observeServerAvailability() {
        scope.launch {
            workspace.customLspServers.collect {
                refreshServerAvailability()
            }
        }
    }


    fun refreshServerAvailability() {
        scope.launch {
            if (currentOperatingSystem != OperatingSystem.Android) return@launch
            val customs = workspace.customLspServers.value.map { it.toConfig() }
            val commands = (LspServerRegistry.all() + customs).map { it.command }.distinct()
            val result = runCatching {
                withTimeoutOrNull(15.seconds) { TermuxBridge.probeTermuxBinaries(commands) }
            }.onFailure {
                if (it is CancellationException) {
                    ensureActive()
                }
            }.getOrNull() ?: emptyMap()
            if (result.isNotEmpty()) {
                _termuxAvailability.value = result
            }
        }
    }

    private fun updateActiveSessions() {
        _activeSessions.value = sessions.mapValues { it.value.state.value }
    }

    override fun close() {
        editorStates.values.forEach { state ->
            state.changeJob?.cancel()
            state.highlightJob?.cancel()
            state.completionJob?.cancel()
            state.disposables.forEach { it.invoke() }
        }
        inlayHintJobs.values.forEach { it.cancel() }
        inlayHintJobs.clear()
        inlayHintGeneration.clear()
        editorStates.clear()
        sessions.values.forEach { it.close() }
        sessions.clear()
        updateActiveSessions()
    }
}
