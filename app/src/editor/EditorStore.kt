package com.klyx.editor

import com.klyx.core.Disposable
import com.klyx.core.action.ActionGroup
import com.klyx.core.action.ActionRegistrar
import com.klyx.core.merge
import com.klyx.runtime.action.InsertText
import com.klyx.runtime.action.Redo
import com.klyx.runtime.action.Undo
import com.klyx.runtime.workspace.DocumentId
import io.github.lumkit.sweeteditor.SweetEditorController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single
class EditorStore : ActionGroup {

    private val live = mutableMapOf<DocumentId, MutableSet<SweetEditorController>>()

    val activeDocumentId: StateFlow<DocumentId?>
        field = MutableStateFlow(null)

    private var activeInstance: SweetEditorController? = null

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

    context(registrar: ActionRegistrar)
    fun registerActions() = registrar.register()

    fun register(id: DocumentId, controller: SweetEditorController) {
        live.getOrPut(id) { mutableSetOf() }.add(controller)
    }

    fun unregister(id: DocumentId, controller: SweetEditorController) {
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
        live[id]?.firstOrNull() ?: SweetEditorController().also { register(id, it) }


    fun controller(id: DocumentId): SweetEditorController = get(id)

    fun setActive(id: DocumentId?, controller: SweetEditorController?) {
        activeInstance = controller
        activeDocumentId.update { id }
        refreshUndoState()
    }

    fun clearActive(controller: SweetEditorController) {
        if (activeInstance === controller) {
            activeInstance = null
            activeDocumentId.update { null }
            refreshUndoState()
        }
    }

    fun remove(id: DocumentId) {
        live.remove(id)
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

    override fun ActionRegistrar.register(): Disposable {
        val disposables = listOf(
            action(Undo) { undo() },
            action(Redo) { redo() },
            action(InsertText) { activeInstance?.insertText(text = it.text) },
        )
        return disposables.merge()
    }
}
