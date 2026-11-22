package com.klyx.lsp

import com.klyx.lsp.types.fold
import com.klyx.runtime.workspace.Document
import com.klyx.ui.editor.LspCompletionItemUi
import com.klyx.ui.editor.scoreCompletion
import io.github.lumkit.sweeteditor.CompletionInsertTextFormat
import io.github.lumkit.sweeteditor.EditorTextEdit
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange
import io.github.lumkit.sweeteditor.CompletionItem as SweetCompletionItem
import io.github.lumkit.sweeteditor.CompletionItemKind as SweetItemKind

internal fun EditorLsp.applyCompletion(item: LspCompletionItemUi, controller: SweetEditorController) {
    dismissCompletionPopup()
    applyCompletionReplacingPrefix(item, controller)
}

internal fun applyCompletionReplacingPrefix(
    item: LspCompletionItemUi,
    controller: SweetEditorController,
) {
    val prefixStart = item.prefixStart
    val prefixEnd = item.prefixEnd
    if (prefixStart == null || prefixEnd == null || prefixStart == prefixEnd) {
        controller.applyCompletionItem(item.sweetItem)
        return
    }

    val rawInsert = item.sweetItem.insertText ?: item.label
    val [finalText, cursorOffset] = expandSnippetPlaceholders(rawInsert)
    val replaceRange = TextRange(
        start = TextPosition(item.line, prefixStart),
        end = TextPosition(item.line, prefixEnd),
    )
    val extra = item.sweetItem.additionalTextEdits.filterNot { (val range, val newText) ->
        range == replaceRange && newText.isEmpty()
    }
    controller.applyTextEdits(
        listOf(EditorTextEdit(range = replaceRange, newText = finalText)) + extra
    )
    controller.setCursorPosition(item.line, prefixStart + cursorOffset)
}

internal fun expandSnippetPlaceholders(template: String): Pair<String, Int> {
    var cursor = template.indexOf("$0")
    var text = template.replace("$0", "")
    if (cursor < 0) cursor = Int.MIN_VALUE
    text = snippetDefaultPattern.replace(text) { it.groupValues[2] }
    text = snippetTabStopPattern.replace(text, "")
    if (cursor == Int.MIN_VALUE) cursor = text.length
    return text to cursor.coerceIn(0, text.length)
}

private val snippetDefaultPattern = Regex("""\$\{(\d+):([^}]*)\}""")
private val snippetTabStopPattern = Regex("""\$(\d+)""")

internal suspend fun EditorLsp.resolveCompletionDoc(
    document: Document,
    item: LspCompletionItemUi
): LspCompletionItemUi {
    val session = sessionForDocument(document) ?: return item
    if (session.state.value != LspSessionState.Running) return item
    if (!item.documentation.isNullOrBlank()) return item
    val resolved = session.resolveCompletionItem(item.rawItem)
    val docText = resolved.documentation?.fold(
        left = { it },
        right = { it.value }
    ) ?: resolved.detail
    return item.copy(
        documentation = docText,
        detail = resolved.detail ?: item.detail,
        labelDetail = resolved.labelDetails?.detail ?: item.labelDetail,
        labelDescription = resolved.labelDetails?.description ?: item.labelDescription,
        rawItem = resolved,
    )
}

internal fun buildSweetCompletionItem(
    item: CompletionItem,
    line: Int,
    prefixStart: Int,
    prefixEnd: Int,
    prefix: String,
): SweetCompletionItem {
    val isSnippet = item.insertTextFormat == InsertTextFormat.Snippet

    val baseAdditionalEdits = item.additionalTextEdits?.map { edit ->
        EditorTextEdit(
            range = TextRange(
                start = TextPosition(edit.range.start.line.toInt(), edit.range.start.character.toInt()),
                end = TextPosition(edit.range.end.line.toInt(), edit.range.end.character.toInt()),
            ),
            newText = edit.newText,
        )
    } ?: emptyList()

    val rawTextEdit = item.textEdit?.fold(
        left = { it },
        right = { (newText, replace) -> TextEdit(replace, newText) }
    )

    val format: CompletionInsertTextFormat
    val insertText: String
    val textEdit: EditorTextEdit?
    val additionalEdits: List<EditorTextEdit>

    if (isSnippet) {
        format = CompletionInsertTextFormat.SNIPPET
        insertText = rawTextEdit?.newText ?: item.insertText ?: item.label
        textEdit = null

        val prefixDeleteEdit = if (rawTextEdit != null) {
            val start =
                TextPosition(rawTextEdit.range.start.line.toInt(), rawTextEdit.range.start.character.toInt())
            val end = TextPosition(rawTextEdit.range.end.line.toInt(), rawTextEdit.range.end.character.toInt())
            if (start != end) {
                listOf(EditorTextEdit(range = TextRange(start = start, end = end), newText = ""))
            } else {
                emptyList()
            }
        } else if (prefixStart < prefixEnd) {
            listOf(
                EditorTextEdit(
                    range = TextRange(
                        start = TextPosition(line, prefixStart),
                        end = TextPosition(line, prefixEnd),
                    ),
                    newText = "",
                )
            )
        } else {
            emptyList()
        }

        additionalEdits = prefixDeleteEdit + baseAdditionalEdits
    } else {
        format = CompletionInsertTextFormat.PLAIN_TEXT
        insertText = item.insertText ?: item.label
        textEdit = rawTextEdit?.let { edit ->
            EditorTextEdit(
                range = TextRange(
                    start = TextPosition(edit.range.start.line.toInt(), edit.range.start.character.toInt()),
                    end = TextPosition(edit.range.end.line.toInt(), edit.range.end.character.toInt()),
                ),
                newText = edit.newText,
            )
        }
        additionalEdits = baseAdditionalEdits
    }

    val sweetKind = completionKind(item.kind)

    val serverSortKey = item.sortText ?: item.label
    val score = scoreCompletion(item, prefix)
    val compositeSortKey = "${score}_$serverSortKey"

    return SweetCompletionItem(
        label = item.label,
        detail = item.detail ?: "",
        insertText = insertText,
        insertTextFormat = format,
        textEdit = textEdit,
        additionalTextEdits = additionalEdits,
        filterText = item.filterText,
        sortKey = compositeSortKey,
        kind = sweetKind,
    )
}


private fun completionKind(kind: CompletionItemKind?) = when (kind) {
    CompletionItemKind.Keyword -> SweetItemKind.KEYWORD
    CompletionItemKind.Function,
    CompletionItemKind.Method,
    CompletionItemKind.Constructor -> SweetItemKind.FUNCTION

    CompletionItemKind.Variable,
    CompletionItemKind.Field,
    CompletionItemKind.Constant,
    CompletionItemKind.Value -> SweetItemKind.VARIABLE

    CompletionItemKind.Class,
    CompletionItemKind.Enum,
    CompletionItemKind.Struct -> SweetItemKind.CLASS

    CompletionItemKind.Interface -> SweetItemKind.INTERFACE
    CompletionItemKind.Module -> SweetItemKind.MODULE
    CompletionItemKind.Property -> SweetItemKind.PROPERTY
    CompletionItemKind.Snippet -> SweetItemKind.SNIPPET
    else -> SweetItemKind.TEXT
}

