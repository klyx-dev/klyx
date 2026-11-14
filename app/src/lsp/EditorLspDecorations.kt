package com.klyx.lsp

import com.klyx.runtime.workspace.Document
import io.github.lumkit.sweeteditor.CodeLensItem
import io.github.lumkit.sweeteditor.DocumentHighlight
import io.github.lumkit.sweeteditor.EditorDocumentHighlightKind
import io.github.lumkit.sweeteditor.EditorTextEdit
import io.github.lumkit.sweeteditor.FoldRegion
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun EditorLsp.formatDocument(document: Document, controller: SweetEditorController): Boolean {
    val session = sessionForDocument(document)
    val uri = LspDocumentUtils.uriOf(document, workspace)
    if (session != null && session.state.value == LspSessionState.Running) {
        val edits = session.formatting(uri)
        if (edits != null) {
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
    }

    // Fallback smart formatting without LSP
    val currentText = withContext(Dispatchers.Main) { controller.getDocument().text }
    val ext = document.name.substringAfterLast('.', "").lowercase()
    val formatted = SmartLanguageFeatures.formatTextFallback(currentText, ext)
    if (formatted != currentText) {
        val lines = currentText.lines()
        val lastLine = (lines.size - 1).coerceAtLeast(0)
        val lastCol = lines.getOrNull(lastLine)?.length ?: 0
        val fullRange = TextRange(
            start = TextPosition(0, 0),
            end = TextPosition(lastLine, lastCol),
        )
        withContext(Dispatchers.Main) {
            controller.applyTextEdits(listOf(EditorTextEdit(range = fullRange, newText = formatted)))
        }
        return true
    }
    return false
}

internal suspend fun EditorLsp.refreshSyntacticFolding(document: Document, controller: SweetEditorController) {
    val text = withContext(Dispatchers.Main) { controller.getDocument().text }
    val docText = text.ifEmpty { workspace.readText(document.id) }
    val regions = SyntacticFolding.computeFoldRegions(docText)
    withContext(Dispatchers.Main) {
        controller.setFoldRegions(regions)
        controller.requestDecorationRefresh()
    }
}

internal suspend fun EditorLsp.refreshFolding(session: LspSession, uri: String, controller: SweetEditorController) {
    val ranges = if (session.state.value == LspSessionState.Running) session.foldingRange(uri) else emptyList()
    if (ranges.isEmpty()) {
        val doc = workspace.documents.value.values.firstOrNull {
            LspDocumentUtils.uriOf(it, workspace) == uri
        }
        if (doc != null) {
            refreshSyntacticFolding(doc, controller)
        } else {
            withContext(Dispatchers.Main) {
                controller.setFoldRegions(emptyList())
                controller.requestDecorationRefresh()
            }
        }
        return
    }

    val validRanges = ranges.filter { (startLine, endLine) -> startLine < endLine }
    val byStartLine = validRanges.groupBy { it.startLine }
    val foldRegions = byStartLine.values.mapNotNull { group ->
        val (startLine, endLine) = group.maxByOrNull { it.endLine } ?: return@mapNotNull null
        FoldRegion(startLine = startLine.toInt(), endLine = endLine.toInt(), collapsed = false)
    }
    withContext(Dispatchers.Main) {
        controller.setFoldRegions(foldRegions)
        controller.requestDecorationRefresh()
    }
}

internal suspend fun EditorLsp.refreshCodeLens(session: LspSession, uri: String, controller: SweetEditorController) {
    if (session.state.value != LspSessionState.Running) return
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
    for ([index, lens] in resolvedLenses.withIndex()) {
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

internal suspend fun refreshDocumentHighlights(
    session: LspSession?,
    uri: String,
    line: Int,
    column: Int,
    controller: SweetEditorController,
) {
    val highlights = if (session != null && session.state.value == LspSessionState.Running) {
        session.documentHighlight(uri, line, column)
    } else emptyList()

    if (highlights.isNotEmpty()) {
        val highlightsByLine = mutableMapOf<Int, MutableList<DocumentHighlight>>()
        for ((range, kind) in highlights) {
            val hLine = range.start.line.toInt()
            val hCol = range.start.character.toInt()
            val hEndCol = range.end.character.toInt()
            val len = if (range.end.line.toInt() == hLine) (hEndCol - hCol).coerceAtLeast(1) else 1
            val hKind = when (kind) {
                DocumentHighlightKind.Read -> EditorDocumentHighlightKind.READ
                DocumentHighlightKind.Write -> EditorDocumentHighlightKind.WRITE
                else -> EditorDocumentHighlightKind.TEXT
            }
            highlightsByLine.getOrPut(hLine) { mutableListOf() }
                .add(DocumentHighlight(column = hCol, length = len, kind = hKind))
        }
        withContext(Dispatchers.Main) {
            controller.setBatchLineDocumentHighlights(highlightsByLine)
            controller.requestDecorationRefresh()
        }
        return
    }

    // Smart fallback occurrence highlighting without LSP
    val docText = withContext(Dispatchers.Main) { controller.getDocument().text }
    val lines = docText.lines()
    val currentLine = lines.getOrNull(line) ?: ""
    val word = SmartLanguageFeatures.getWordAt(currentLine, column)

    if (word.length >= 2 && (word[0].isLetter() || word[0] == '_')) {
        val wordRegex = Regex("""\b${Regex.escape(word)}\b""")
        val highlightsByLine = mutableMapOf<Int, MutableList<DocumentHighlight>>()
        for ([idx, l] in lines.withIndex()) {
            val matches = wordRegex.findAll(l)
            for (m in matches) {
                highlightsByLine.getOrPut(idx) { mutableListOf() }
                    .add(
                        DocumentHighlight(
                            column = m.range.first,
                            length = word.length,
                            kind = EditorDocumentHighlightKind.TEXT
                        )
                    )
            }
        }
        withContext(Dispatchers.Main) {
            controller.setBatchLineDocumentHighlights(highlightsByLine)
            controller.requestDecorationRefresh()
        }
    } else {
        withContext(Dispatchers.Main) {
            controller.clearDocumentHighlights()
            controller.requestDecorationRefresh()
        }
    }
}

