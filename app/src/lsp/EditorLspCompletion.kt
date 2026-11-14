package com.klyx.lsp

import com.klyx.lsp.types.fold
import com.klyx.runtime.workspace.Document
import com.klyx.ui.editor.CompletionUiState
import com.klyx.ui.editor.LspCompletionItemUi
import com.klyx.ui.editor.scoreCompletion
import io.github.lumkit.sweeteditor.SweetEditorController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

internal fun EditorLsp.triggerCompletionPopup(document: Document, controller: SweetEditorController) {
    val uri = LspDocumentUtils.uriOf(document, workspace)
    val state = editorStates.getOrPut(document.id) { EditorLsp.EditorState() }
    state.completionJob?.cancel()
    state.completionJob = scope.launch {
        delay(50.milliseconds)
        val (line, column) = withContext(Dispatchers.Main) { controller.getCursorPosition() } ?: return@launch
        val fullText = withContext(Dispatchers.Main) { controller.getDocument().text }
        val lines = fullText.lines()
        val lineStr = lines.getOrNull(line) ?: ""
        val col = column.coerceIn(0, lineStr.length)
        var prefixStart = col
        while (prefixStart > 0 && (lineStr[prefixStart - 1].isLetterOrDigit() || lineStr[prefixStart - 1] == '_')) {
            prefixStart--
        }
        val prefix = lineStr.substring(prefixStart, col)

        val rawItems = collectCompletionItems(document, uri, fullText, line, column, prefix)

        if (rawItems.isEmpty()) {
            _activeCompletion.value = null
            return@launch
        }

        val filteredItems = if (prefix.isEmpty()) {
            rawItems
        } else {
            rawItems.filter { (val label, val filterText) ->
                label.contains(prefix, ignoreCase = true) ||
                        filterText?.contains(prefix, ignoreCase = true) == true
            }
        }
        if (filteredItems.isEmpty()) {
            _activeCompletion.value = null
            return@launch
        }

        val uiItems = completionRows(filteredItems, line, prefixStart, col, prefix)

        val anchor = withContext(Dispatchers.Main) { controller.getCursorRect() }
        _activeCompletion.value = CompletionUiState(
            documentId = document.id,
            items = uiItems,
            selectedIndex = 0,
            anchor = anchor,
        )
    }
}

private suspend fun EditorLsp.collectCompletionItems(
    document: Document,
    uri: String,
    fullText: String,
    line: Int,
    column: Int,
    prefix: String,
): List<CompletionItem> {
    val session = sessionForDocument(document)
    val rawItems = mutableListOf<CompletionItem>()
    val seenLabels = mutableSetOf<String>()

    if (session != null && session.state.value == LspSessionState.Running) {
        session.syncTextIfNeeded(uri, fullText)
        val lspItems = runCatching { session.completion(uri, line, column) }.getOrNull().orEmpty()
        rawItems.addAll(lspItems)
        lspItems.forEach { seenLabels.add(it.label) }
    }

    val languageId = LspDocumentUtils.languageIdOf(document)
    val localItems = localCompletionItems(languageId, fullText, prefix)
    rawItems += localItems.filter { seenLabels.add(it.label) }

    return rawItems
}

private fun EditorLsp.completionRows(
    filteredItems: List<CompletionItem>,
    line: Int,
    prefixStart: Int,
    col: Int,
    prefix: String,
): List<LspCompletionItemUi> = filteredItems.map { item ->
    val sweetItem = buildSweetCompletionItem(item, line, prefixStart, col, prefix)
    val docText = item.documentation?.fold(
        left = { it },
        right = { it.value }
    )
    @Suppress("DEPRECATION")
    val isDeprecated = item.deprecated == true || item.tags?.contains(CompletionItemTag.Deprecated) == true
    val isSnippet = item.insertTextFormat == InsertTextFormat.Snippet

    LspCompletionItemUi(
        label = item.label,
        detail = item.detail,
        labelDetail = item.labelDetails?.detail,
        labelDescription = item.labelDetails?.description,
        documentation = docText,
        kind = item.kind,
        isDeprecated = isDeprecated,
        isSnippet = isSnippet,
        rawItem = item,
        sweetItem = sweetItem,
        line = line,
        prefixStart = item.textEdit?.let { edit ->
            edit.fold(
                left = { it.range.start.character.toInt() },
                right = { it.replace.start.character.toInt() },
            )
        } ?: prefixStart,
        prefixEnd = col,
    )
}.sortedWith(
    compareBy<LspCompletionItemUi> { item ->
        scoreCompletion(item.rawItem, prefix)
    }.thenBy { (val label, val rawItem) ->
        rawItem.sortText ?: label
    }.thenBy { item ->
        item.label
    }
)

private fun localCompletionItems(languageId: String, text: String, prefix: String): List<CompletionItem> {
    val keywords = SmartLanguageFeatures.keywordsFor(languageId).map { keyword ->
        CompletionItem(label = keyword, kind = CompletionItemKind.Keyword, detail = "keyword", insertText = keyword)
    }
    val snippets = SmartLanguageFeatures.snippetsFor(languageId).map { (label, detail, body) ->
        CompletionItem(
            label = label,
            kind = CompletionItemKind.Snippet,
            detail = detail,
            insertText = body,
            insertTextFormat = InsertTextFormat.Snippet,
        )
    }
    val words = SmartLanguageFeatures.extractWords(text, prefix)
        .filter { it != prefix }
        .map { word ->
            CompletionItem(label = word, kind = CompletionItemKind.Text, detail = "buffer", insertText = word)
        }
    return (keywords + snippets).filter {
        prefix.isEmpty() || it.label.contains(prefix, ignoreCase = true)
    } + words
}
