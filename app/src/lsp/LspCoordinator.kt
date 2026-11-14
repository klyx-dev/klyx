package com.klyx.lsp

import com.klyx.document.DocumentPersistence
import com.klyx.editor.EditorStore
import com.klyx.runtime.settings.SettingsStore
import com.klyx.runtime.workspace.Document
import com.klyx.runtime.workspace.DocumentId
import com.klyx.runtime.workspace.Workspace
import com.klyx.ui.editor.CompletionUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class HoverState(
    val line: Int,
    val column: Int,
    val content: String,
)

@Single
class EditorLsp(
    val workspace: Workspace,
    val editorStore: EditorStore,
    internal val settingsStore: SettingsStore,
    internal val documents: DocumentPersistence,
) : AutoCloseable {

    internal class EditorState {
        val disposables = mutableListOf<() -> Unit>()
        var changeJob: Job? = null
        var highlightJob: Job? = null
        var completionJob: Job? = null
    }

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val client = LspClientImpl(workspace, editorStore)

    internal val sessions = mutableMapOf<String, LspSession>()
    internal val editorStates = mutableMapOf<DocumentId, EditorState>()
    internal val inlayHintJobs = mutableMapOf<String, Job>()
    internal val inlayHintGeneration = mutableMapOf<String, Int>()
    internal val documentCodeLenses = mutableMapOf<DocumentId, List<CodeLens>>()

    val activeHover: StateFlow<HoverState?>
        field = MutableStateFlow<HoverState?>(null)

    fun dismissHover() {
        activeHover.value = null
    }

    val activeCompletion: StateFlow<CompletionUiState?>
        field = MutableStateFlow<CompletionUiState?>(null)

    fun dismissCompletionPopup() {
        editorStates.values.forEach { it.completionJob?.cancel() }
        activeCompletion.value = null
    }

    val activeSessions: StateFlow<Map<String, LspSessionState>>
        field = MutableStateFlow<Map<String, LspSessionState>>(emptyMap())

    val lspErrors: SharedFlow<String>
        field = MutableSharedFlow<String>(extraBufferCapacity = 8)

    val termuxAvailability: StateFlow<Map<String, Boolean>>
        field = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            documents.savedDocuments.collect { (document, text) ->
                notifyDocumentSaved(document, text)
            }
        }
        observeWorkspaceProjects()
        observeInlayHintsSetting()
        observeCustomServers()
        observeServerAvailability()
        setupClientCallbacks()
    }

    internal fun setupClientCallbacks() {
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

    internal fun refreshActiveDiagnostics() {
        scope.launch {
            val activeDocId = editorStore.activeDocumentId.value ?: return@launch
            val document = workspace.document(activeDocId) ?: return@launch
            val session = sessionForDocument(document) ?: return@launch
            if (session.state.value == LspSessionState.Running) {
                refreshDiagnostics(session, LspDocumentUtils.uriOf(document, workspace))
            }
        }
    }

    internal suspend fun refreshDiagnostics(
        session: LspSession,
        uri: String,
    ) {
        if (session.state.value != LspSessionState.Running) return
        val pulled = session.pullDiagnostics(uri) ?: return
        client.setDiagnostics(uri, pulled)
    }

    internal fun refreshActiveDecorations(
        inlay: Boolean = false,
        codeLens: Boolean = false,
        targetUri: String? = null,
        inlayDebounce: Duration = 0.milliseconds,
    ) {
        scope.launch {
            val activeDocId = editorStore.activeDocumentId.value ?: return@launch
            val doc = workspace.document(activeDocId) ?: return@launch
            val session = sessionForDocument(doc) ?: return@launch
            if (session.state.value != LspSessionState.Running) return@launch
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

    suspend fun notifyDocumentSaved(document: Document, text: String? = null) {
        val session = sessionForDocument(document)
        val uri = LspDocumentUtils.uriOf(document, workspace)
        val docText = text ?: workspace.readText(document.id)
        if (session != null && session.state.value == LspSessionState.Running) {
            session.didSave(uri, docText)
            refreshDiagnostics(session, uri)
            val _ = editorStore.withDocument(document.id) {
                val controller = this@withDocument
                scope.launch {
                    refreshFolding(session, uri, controller)
                    refreshCodeLens(session, uri, controller)
                }
            }
        } else {
            val _ = editorStore.withDocument(document.id) {
                val controller = this@withDocument
                scope.launch {
                    refreshSyntacticFolding(document, controller)
                }
            }
        }
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
        scope.cancel()
    }
}
