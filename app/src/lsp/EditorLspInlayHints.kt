package com.klyx.lsp

import com.klyx.lsp.types.fold
import io.github.lumkit.sweeteditor.SweetEditorController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal fun EditorLsp.scheduleInlayHintRefresh(
    session: LspSession,
    uri: String,
    controller: SweetEditorController,
    debounce: Duration = 0.milliseconds,
) {
    if (!settingsStore.settings.value.showInlayHints || session.state.value != LspSessionState.Running) {
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

internal suspend fun EditorLsp.refreshInlayHints(
    session: LspSession,
    uri: String,
    controller: SweetEditorController,
    generation: Int,
) {
    if (!session.supportsInlayHints() || session.state.value != LspSessionState.Running) return
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
    if (!settingsStore.settings.value.showInlayHints) return

    val currentLines = currentText.lines()
    val hintsByLine = mutableMapOf<Int, MutableList<io.github.lumkit.sweeteditor.InlayHint>>()
    if (hints.isNotEmpty()) {
        for ((position, label, paddingLeft, paddingRight) in hints) {
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

