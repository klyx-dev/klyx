package com.klyx.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.klyx.core.Disposable
import com.klyx.core.action.Action
import com.klyx.core.action.ActionRegistrar
import com.klyx.core.action.action
import com.klyx.core.asDisposable
import com.klyx.runtime.action.ClearSearch
import com.klyx.runtime.action.Copy
import com.klyx.runtime.action.Cut
import com.klyx.runtime.action.FindNext
import com.klyx.runtime.action.FindPrevious
import com.klyx.runtime.action.FoldAll
import com.klyx.runtime.action.GotoLine
import com.klyx.runtime.action.InsertSnippet
import com.klyx.runtime.action.InsertText
import com.klyx.runtime.action.Paste
import com.klyx.runtime.action.Redo
import com.klyx.runtime.action.SelectAll
import com.klyx.runtime.action.ToggleFold
import com.klyx.runtime.action.TriggerCompletion
import com.klyx.runtime.action.Undo
import com.klyx.runtime.action.UnfoldAll
import com.klyx.runtime.workspace.DocumentId
import io.github.lumkit.sweeteditor.SweetEditorController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

data class EditorCursor(val line: Int, val column: Int)

internal data class DocumentJump(val documentId: DocumentId, val line: Int, val column: Int)

@Single
class EditorStore : ActionRegistrar {

    internal var pendingJump by mutableStateOf<DocumentJump?>(null)

    private val live = mutableMapOf<DocumentId, MutableSet<SweetEditorController>>()
    private val registrations = mutableMapOf<SweetEditorController, Int>()

    val activeDocumentId: StateFlow<DocumentId?>
        field = MutableStateFlow(null)

    private var activeInstance: SweetEditorController? = null

    val activeCursor: StateFlow<EditorCursor?>
        field = MutableStateFlow(null)

    fun updateCursor(line: Int, column: Int) {
        activeCursor.value = EditorCursor(line, column)
    }

    fun clearCursor() {
        activeCursor.value = null
    }

    val canUndo: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val canRedo: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun refreshUndoState() {
        canUndo.value = activeInstance?.canUndo() == true
        canRedo.value = activeInstance?.canRedo() == true
    }

    val activeController: SweetEditorController?
        get() = activeInstance

    fun register(id: DocumentId, controller: SweetEditorController) {
        registrations[controller] = (registrations[controller] ?: 0) + 1
        live.getOrPut(id) { mutableSetOf() }.add(controller)
    }

    fun unregister(id: DocumentId, controller: SweetEditorController) {
        val count = registrations[controller] ?: 0
        if (count > 1) {
            registrations[controller] = count - 1
            return
        }
        registrations.remove(controller)
        live[id]?.remove(controller)
        if (live[id].isNullOrEmpty()) live.remove(id)
        if (activeInstance === controller) {
            activeInstance = null
            activeDocumentId.update { null }
            refreshUndoState()
        }
    }

    fun controllersFor(id: DocumentId): Set<SweetEditorController> =
        live[id]?.toSet() ?: emptySet()

    operator fun get(id: DocumentId): SweetEditorController =
        live[id]?.firstOrNull() ?: SweetEditorController().also { live.getOrPut(id) { mutableSetOf() }.add(it) }

    fun controller(id: DocumentId): SweetEditorController = get(id)

    fun setActive(id: DocumentId?, controller: SweetEditorController?) {
        activeInstance = controller
        activeDocumentId.update { id }
        refreshUndoState()
        val pos = controller?.getCursorPosition()
        if (pos != null) {
            activeCursor.value = EditorCursor(pos.line, pos.column)
        } else {
            activeCursor.value = null
        }
    }

    fun clearActive(controller: SweetEditorController) {
        if (activeInstance === controller) {
            activeInstance = null
            activeDocumentId.update { null }
            activeCursor.value = null
            refreshUndoState()
        }
    }

    fun remove(id: DocumentId) {
        live.remove(id)?.forEach { registrations.remove(it) }
        if (activeDocumentId.value == id) {
            activeInstance = null
            activeDocumentId.update { null }
        }
    }

    fun withDocument(id: DocumentId, block: SweetEditorController.() -> Unit): Boolean {
        val targets = controllersFor(id)
        if (targets.isEmpty()) return false
        targets.forEach { it.block() }
        return true
    }

    fun withActive(block: SweetEditorController.() -> Unit) {
        activeInstance?.block()
    }

    fun forEach(block: (DocumentId, SweetEditorController) -> Unit) {
        live.toMap().forEach { entry ->
            entry.value.toList().forEach { controller -> block(entry.key, controller) }
        }
    }

    fun undo() {
        activeInstance?.undo()
        refreshUndoState()
    }

    fun redo() {
        activeInstance?.redo()
        refreshUndoState()
    }

    private inline fun <reified A : Action> editorAction(noinline handler: (A) -> Unit) =
        action<A> { request -> withContext(Dispatchers.Main.immediate) { handler(request) } }

    override fun registerActions(): Disposable {
        val registrations = listOf(
            editorAction<Undo> { undo() },
            editorAction<Redo> { redo() },
            editorAction<InsertText> { activeInstance?.insertText(text = it.text) },
            editorAction<SelectAll> { activeInstance?.selectAll() },
            editorAction<Copy> { activeInstance?.copyToClipboard() },
            editorAction<Cut> { activeInstance?.cutToClipboard() },
            editorAction<Paste> { activeInstance?.pasteFromClipboard() },
            editorAction<FoldAll> { activeInstance?.foldAll() },
            editorAction<UnfoldAll> { activeInstance?.unfoldAll() },
            editorAction<ToggleFold> { activeInstance?.toggleFold(it.line) },
            editorAction<TriggerCompletion> { activeInstance?.triggerCompletion() },
            editorAction<InsertSnippet> { activeInstance?.insertSnippet(it.snippet) },
            editorAction<GotoLine> { activeInstance?.gotoPosition(it.line, it.column) },
            editorAction<FindNext> { activeInstance?.findNextSearchMatch() },
            editorAction<FindPrevious> { activeInstance?.findPreviousSearchMatch() },
            editorAction<ClearSearch> { activeInstance?.clearSearch() },
        )
        return registrations.asDisposable()
    }
}
