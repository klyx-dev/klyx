package com.klyx.lsp

import com.klyx.core.Disposable
import com.klyx.core.action.Action
import com.klyx.core.action.ActionGroup
import com.klyx.core.action.ActionId
import com.klyx.core.action.ActionRegistrar
import com.klyx.core.merge
import com.klyx.runtime.action.SaveActiveDocument
import com.klyx.runtime.action.SaveAllDocuments
import com.klyx.runtime.action.SaveDocument
import org.koin.core.annotation.Single

object FormatDocument : Action {
    override val id = ActionId("lsp.formatDocument")
}

object RestartLanguageServer : Action {
    override val id = ActionId("lsp.restartLanguageServer")
}

@Single
class LspActionGroup(
    private val coordinator: EditorLsp,
) : ActionGroup {

    context(registrar: ActionRegistrar)
    fun registerActions() = registrar.register()

    override fun ActionRegistrar.register(): Disposable {
        val disposables = listOf(
            action(
                instance = SaveActiveDocument,
                title = "Save",
                defaultKeybinding = "Ctrl+S",
                serialize = true,
            ) {
                coordinator.saveActiveDocument()
            },
            action(
                descriptor = SaveDocument,
                title = "Save Document",
                serialize = true,
            ) {
                coordinator.saveDocument(it.documentId)
            },
            action(
                instance = SaveAllDocuments,
                title = "Save All",
                defaultKeybinding = "Ctrl+Shift+S",
                serialize = true,
            ) {
                coordinator.saveAllDocuments()
            },
            action(FormatDocument) {
                val activeDocId = coordinator.editorStore.activeDocumentId.value ?: return@action
                val doc = coordinator.workspace.document(activeDocId) ?: return@action
                val controller = coordinator.editorStore.activeController ?: return@action
                val _ = coordinator.formatDocument(doc, controller)
            },
            action(RestartLanguageServer) {
                val activeDocId = coordinator.editorStore.activeDocumentId.value ?: return@action
                val doc = coordinator.workspace.document(activeDocId) ?: return@action
                val _ = coordinator.restartLanguageServerForDocument(doc)
            }
        )
        return disposables.merge()
    }
}
