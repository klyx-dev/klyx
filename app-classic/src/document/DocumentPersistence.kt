package com.klyx.document

import com.klyx.core.App
import com.klyx.editor.EditorStore
import com.klyx.lsp.termuxSnapshotSource
import com.klyx.runtime.settings.SettingsStore
import com.klyx.runtime.settings.settingsFile
import com.klyx.runtime.workspace.Document
import com.klyx.runtime.workspace.DocumentId
import com.klyx.runtime.workspace.Workspace
import com.klyx.runtime.workspace.WorkspaceSessionManager
import com.klyx.runtime.workspace.workspaceSessionFile
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val sessions: WorkspaceSessionManager,
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val bindings = mutableMapOf<SweetEditorController, () -> Unit>()
    private val bindingCounts = mutableMapOf<SweetEditorController, Int>()
    private val pendingSaves = mutableMapOf<SweetEditorController, Job>()
    val error = MutableStateFlow<String?>(null)
    private val writes = Mutex()
    private val saved = MutableSharedFlow<SavedDocument>()
    val savedDocuments = saved.asSharedFlow()

    fun isReadOnly(document: Document): Boolean {
        val file = workspace.resolve(document)
        return termuxSnapshotSource(file) != null || isWorkspaceSessionFile(file, App.global.workspaceSessionFile)
    }

    fun bindEditor(document: Document, controller: SweetEditorController) {
        if (isReadOnly(document)) return
        val count = bindingCounts[controller] ?: 0
        bindingCounts[controller] = count + 1
        if (count > 0) return
        bindings[controller] = controller.onTextChanged {
            workspace.updateBuffer(document.id, controller.getDocument().text)
            pendingSaves.remove(controller)?.cancel()
            pendingSaves[controller] = scope.launch {
                delay(500.milliseconds)
                if (
                    settings.settings.value.autoSave &&
                    workspace.pendingClose.value == null &&
                    workspace.isDirty(document.id)
                ) {
                    save(document, controller.getDocument().text)
                }
            }
        }
    }

    fun unbindEditor(controller: SweetEditorController) {
        val count = bindingCounts[controller] ?: return
        if (count > 1) {
            bindingCounts[controller] = count - 1
            return
        }
        bindingCounts.remove(controller)
        bindings.remove(controller)?.invoke()
        pendingSaves.remove(controller)?.cancel()
    }

    suspend fun loadDocument(document: Document, controller: SweetEditorController, onLoaded: () -> Unit) {
        try {
            sessions.awaitRestored()
            val readOnly = isReadOnly(document)
            val text = if (readOnly) {
                workspace.readText(document.id)
            } else workspace.buffers.value[document.id]?.text ?: if (isSettingsFile(document)) {
                settings.readJson()
            } else {
                workspace.readText(document.id)
            }
            withContext(Dispatchers.Main) {
                workspace.initializeBuffer(document.id, text)
                if (readOnly) {
                    workspace.updateBuffer(document.id, text)
                    workspace.markSaved(document.id, text)
                }
                controller.whenReady {
                    controller.loadDocument(text = workspace.buffers.value[document.id]?.text ?: text)
                    onLoaded()
                }
            }
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            error.value = "Could not open ${document.name}: ${failure.message}"
        }
    }

    suspend fun saveDocument(documentId: DocumentId) {
        if (!workspace.isDirty(documentId)) return
        val document = workspace.document(documentId) ?: return
        val text = workspace.buffers.value[documentId]?.text ?: return
        save(document, text)
    }

    suspend fun resolveClose(discard: Boolean) {
        val request = workspace.pendingClose.value ?: return
        error.value = null
        pendingSaves.values.forEach { it.cancel() }
        pendingSaves.clear()
        if (discard) {
            writes.withLock {
                // Other splits may still display this document after one split closes.
                request.documentIds.forEach { id ->
                    val text = workspace.buffers.value[id]?.savedText ?: return@forEach
                    editors.controllersFor(id).forEach { it.loadDocument(text = text) }
                }
                workspace.finishClose(discard = true)
            }
        } else {
            request.documentIds.forEach { saveDocument(it) }
            if (error.value == null) workspace.finishClose(discard = false)
        }
    }

    fun requestExit(exit: () -> Unit) {
        scope.launch {
            try {
                sessions.awaitRestored()
                workspace.requestClose(workspace.documents.value.keys) {
                    scope.launch { flushAndExit(exit) }
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                error.value = "Could not restore the workspace: ${failure.message}"
            }
        }
    }

    private suspend fun flushAndExit(exit: () -> Unit) {
        try {
            sessions.saveCurrentSession()
            // An edit may have arrived while the backup was being written.
            if (workspace.buffers.value.values.any { it.isDirty }) requestExit(exit)
            else exit()
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            error.value = "Could not back up the workspace: ${failure.message}"
        }
    }

    suspend fun saveActiveDocument() {
        (editors.activeDocumentId.value ?: workspace.activeGroupOrNull()?.activeDocumentId)
            ?.let { saveDocument(it) }
    }

    suspend fun saveAllDocuments() {
        workspace.documents.value.keys.toList().forEach { saveDocument(it) }
    }

    private suspend fun save(document: Document, text: String) {
        try {
            check(!isReadOnly(document)) { "${document.name} is read-only" }
            writes.withLock {
                withContext(NonCancellable) {
                    if (isSettingsFile(document)) {
                        settings.writeJson(text)
                    } else {
                        workspace.writeText(document.id, text)
                    }
                    withContext(Dispatchers.Main) { workspace.markSaved(document.id, text) }
                }
                sessions.saveCurrentSession()
                saved.emit(SavedDocument(document, text))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            this.error.value = "Could not save ${document.name}: ${error.message}"
        }
    }

    private fun isSettingsFile(document: Document): Boolean =
        workspace.resolve(document).path == App.global.settingsFile.path

    override fun close() {
        bindings.values.forEach { it() }
        bindings.clear()
        bindingCounts.clear()
        pendingSaves.clear()
        scope.cancel()
    }
}
