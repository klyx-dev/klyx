package com.klyx.lsp

import com.klyx.runtime.workspace.Document
import com.klyx.ui.editor.CompletionItemRow
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.TextChangeKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlin.time.Duration.Companion.milliseconds

internal fun EditorLsp.bindEditor(document: Document, controller: SweetEditorController) {
    val state = editorStates.getOrPut(document.id) { EditorLsp.EditorState() }
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

    bindTextChanges(document, controller, state, uri)
    bindPointerEvents(document, controller, state, uri)
    openEditorDocument(document, controller, uri, langId)
}

private fun EditorLsp.bindTextChanges(
    document: Document,
    controller: SweetEditorController,
    state: EditorLsp.EditorState,
    uri: String,
) {
    val unbindList = state.disposables
    val unsubText = controller.onTextChanged { (val changes, val kind) ->
        scheduleDocumentRefresh(document, controller, state, uri)

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
    unbindList.add(unsubText)

}

private fun EditorLsp.bindPointerEvents(
    document: Document,
    controller: SweetEditorController,
    state: EditorLsp.EditorState,
    uri: String,
) {
    val unbindList = state.disposables
    val unsubLongPress = controller.onLongPress { event ->
        scope.launch {
            val _ = requestHover(document, event.cursorPosition.line, event.cursorPosition.column)
        }
    }
    unbindList.add(unsubLongPress)

    val unsubCursor = controller.onCursorChanged { event ->
        if (activeHover.value?.let { it.line != event.cursorPosition.line } == true) {
            dismissHover()
        }
        if (activeCompletion.value?.let { it.anchor == null } == true) {
            dismissCompletionPopup()
        }
        state.highlightJob?.cancel()
        state.highlightJob = scope.launch {
            delay(120.milliseconds)
            refreshDocumentHighlights(
                sessionForDocument(document),
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
        val (command, arguments) = lens?.command ?: return@onCodeLensClick
        scope.launch {
            val session = sessionForDocument(document) ?: return@launch
            val args = (arguments as? JsonArray)?.toList()
            val _ = session.executeCommand(command, args)
        }
    }
    unbindList.add(unsubCodeLens)

}

private fun EditorLsp.openEditorDocument(
    document: Document,
    controller: SweetEditorController,
    uri: String,
    langId: String,
) {
    controller.whenReady {
        scope.launch {
            refreshSyntacticFolding(document, controller)
        }
    }

    if (shouldLaunchLspFor(document)) {
        scope.launch {
            val session = ensureSession(document, silent = true) ?: return@launch

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
        }
    }
}

internal fun EditorLsp.unbindEditor(document: Document) {
    val state = editorStates.remove(document.id)
    state?.disposables?.forEach { it.invoke() }
    state?.changeJob?.cancel()
    state?.highlightJob?.cancel()
    state?.completionJob?.cancel()
    val docUri = LspDocumentUtils.uriOf(document, workspace)
    inlayHintJobs.remove(docUri)?.cancel()
    inlayHintGeneration.remove(docUri)
    if (activeCompletion.value?.documentId == document.id) {
        updateCompletion(null)
    }
    documentCodeLenses.remove(document.id)

    scope.launch {
        val session = sessionForDocument(document) ?: return@launch
        if (session.state.value == LspSessionState.Running) {
            val uri = LspDocumentUtils.uriOf(document, workspace)
            session.didClose(uri)
        }
    }
}


private fun EditorLsp.scheduleDocumentRefresh(
    document: Document,
    controller: SweetEditorController,
    state: EditorLsp.EditorState,
    uri: String,
) {
    state.changeJob?.cancel()
    state.changeJob = scope.launch {
        delay(500.milliseconds)
        val latestText = controller.getDocument().text
        val liveSession = sessionForDocument(document)
        if (liveSession != null && liveSession.state.value == LspSessionState.Running) {
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
        } else {
            refreshSyntacticFolding(document, controller)
        }
    }
}
