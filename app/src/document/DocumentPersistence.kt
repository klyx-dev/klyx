package com.klyx.document

import com.klyx.editor.EditorStore
import com.klyx.runtime.settings.SettingsStore
import com.klyx.runtime.workspace.Document
import com.klyx.runtime.workspace.DocumentId
import com.klyx.runtime.workspace.Workspace
import io.github.lumkit.sweeteditor.SweetEditorController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.milliseconds

data class SavedDocument(val document: Document, val text: String)

/** Owns disk writes and editor save policy independently of language servers. */
@Single
class DocumentPersistence(
    private val workspace: Workspace,
    private val editors: EditorStore,
    private val settings: SettingsStore,
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val bindings = mutableMapOf<SweetEditorController, () -> Unit>()
    private val pendingSaves = mutableMapOf<SweetEditorController, Job>()
    private val saved = MutableSharedFlow<SavedDocument>()
    val savedDocuments = saved.asSharedFlow()

    fun bindEditor(document: Document, controller: SweetEditorController) {
        bindings.remove(controller)?.invoke()
        pendingSaves.remove(controller)?.cancel()
        bindings[controller] = controller.onTextChanged {
            pendingSaves.remove(controller)?.cancel()
            pendingSaves[controller] = scope.launch {
                delay(500.milliseconds)
                if (settings.settings.value.autoSave) {
                    save(document, controller.getDocument().text)
                }
            }
        }
    }

    fun unbindEditor(document: Document, controller: SweetEditorController) {
        bindings.remove(controller)?.invoke()
        pendingSaves.remove(controller)?.cancel()
        // Capture the buffer before the controller is unregistered or disposed.
        val text = controller.getDocument().text
        scope.launch { save(document, text) }
    }

    suspend fun saveDocument(documentId: DocumentId) {
        val document = workspace.document(documentId) ?: return
        val text = withContext(Dispatchers.Main) {
            editors.controllersFor(documentId).firstOrNull()?.getDocument()?.text
        } ?: workspace.readText(documentId)
        save(document, text)
    }

    suspend fun saveActiveDocument() {
        editors.activeDocumentId.value?.let { saveDocument(it) }
    }

    suspend fun saveAllDocuments() {
        workspace.documents.value.keys.toList().forEach { saveDocument(it) }
    }

    private suspend fun save(document: Document, text: String) {
        try {
            workspace.writeText(document.id, text)
            saved.emit(SavedDocument(document, text))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            println("[Save ERROR] Failed to save ${document.name}: ${error.message}")
        }
    }

    override fun close() {
        bindings.values.forEach { it() }
        bindings.clear()
        pendingSaves.clear()
        scope.cancel()
    }
}
