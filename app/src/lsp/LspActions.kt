package com.klyx.lsp

import com.klyx.core.Disposable
import com.klyx.core.action.Action
import com.klyx.core.action.ActionRegistrar
import com.klyx.core.action.action
import com.klyx.core.asDisposable
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Serializable
object FormatDocument : Action

@Serializable
object RestartLanguageServer : Action

@Single
class LspActions(private val coordinator: EditorLsp) : ActionRegistrar {

    override fun registerActions(): Disposable {
        val registrations = listOf(
            action<FormatDocument> {
                val activeDocId = coordinator.editorStore.activeDocumentId.value ?: return@action
                val doc = coordinator.workspace.document(activeDocId) ?: return@action
                val controller = coordinator.editorStore.activeController ?: return@action
                val _ = coordinator.formatDocument(doc, controller)
            },
            action<RestartLanguageServer> {
                val activeDocId = coordinator.editorStore.activeDocumentId.value ?: return@action
                val doc = coordinator.workspace.document(activeDocId) ?: return@action
                val _ = coordinator.restartLanguageServerForDocument(doc)
            }
        )
        return registrations.asDisposable()
    }
}
