package com.klyx.lsp

import com.klyx.lsp.types.fold
import com.klyx.ui.editor.scoreCompletion
import io.github.lumkit.sweeteditor.CompletionContext
import io.github.lumkit.sweeteditor.CompletionInsertTextFormat
import io.github.lumkit.sweeteditor.CompletionItem
import io.github.lumkit.sweeteditor.CompletionProvider
import io.github.lumkit.sweeteditor.CompletionReceiver
import io.github.lumkit.sweeteditor.CompletionResult
import io.github.lumkit.sweeteditor.EditorTextEdit
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.github.lumkit.sweeteditor.CompletionItemKind as SweetCompletionItemKind

class LspCompletionProvider(
    private val sessionProvider: () -> LspSession?,
    private val uriProvider: () -> String,
    private val textProvider: (() -> String)? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : CompletionProvider {

    override fun isTriggerCharacter(ch: String): Boolean {
        if (ch.any { it.isLetterOrDigit() || it == '_' || it == '$' }) return true
        val session = sessionProvider() ?: return false
        val triggerChars = session.capabilities?.completionProvider?.triggerCharacters ?: return false
        return triggerChars.contains(ch)
    }

    override fun provideCompletions(context: CompletionContext, receiver: CompletionReceiver) {
        val session = sessionProvider() ?: return
        val uri = uriProvider()

        (val line, val col = column) = context.cursorPosition

        scope.launch {
            if (receiver.isCancelled) return@launch

            textProvider?.invoke()?.let { currentText ->
                session.syncTextIfNeeded(uri, currentText)
            }

            val items = session.completion(uri, line, col)
            if (receiver.isCancelled) return@launch

            val lineStr = context.lineText
            val prefixEnd = col.coerceIn(0, lineStr.length)
            var prefixStart = prefixEnd
            while (prefixStart > 0 && (lineStr[prefixStart - 1].isLetterOrDigit() || lineStr[prefixStart - 1] == '_')) {
                prefixStart--
            }
            val prefix = lineStr.substring(prefixStart, prefixEnd)

            val filteredItems = if (prefix.isEmpty()) {
                items
            } else {
                items.filter { (val label, val filterText) ->
                    label.contains(prefix, ignoreCase = true) ||
                        filterText?.contains(prefix, ignoreCase = true) == true
                }
            }

            val sweetItems = filteredItems.map { item ->
                val kind = mapCompletionItemKind(item.kind)
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
                    right = { (newText, insert) -> TextEdit(insert, newText) }
                )

                val format: CompletionInsertTextFormat
                val insertText: String
                val textEdit: EditorTextEdit?
                val additionalEdits: List<EditorTextEdit>

                val serverEditRange = item.textEdit?.let { edit ->
                    edit.fold(
                        left = { it.range },
                        right = { it.replace },
                    )
                }
                val useServerRange = serverEditRange != null &&
                    serverEditRange.start.line.toInt() == line &&
                    serverEditRange.end.line.toInt() == line &&
                    serverEditRange.end.character.toInt() > serverEditRange.start.character.toInt()

                val replaceText = rawTextEdit?.newText ?: item.insertText ?: item.label
                val replaceRange = if (useServerRange) {
                    TextRange(
                        start = TextPosition(serverEditRange.start.line.toInt(), serverEditRange.start.character.toInt()),
                        end = TextPosition(serverEditRange.end.line.toInt(), serverEditRange.end.character.toInt()),
                    )
                } else {
                    TextRange(
                        start = TextPosition(line, prefixStart),
                        end = TextPosition(line, prefixEnd),
                    )
                }
                val hasWordToReplace = useServerRange || prefixStart < prefixEnd

                if (isSnippet) {
                    format = CompletionInsertTextFormat.SNIPPET
                    insertText = replaceText
                    textEdit = null
                    additionalEdits = if (hasWordToReplace) {
                        listOf(EditorTextEdit(range = replaceRange, newText = "")) + baseAdditionalEdits
                    } else {
                        baseAdditionalEdits
                    }
                } else {
                    format = CompletionInsertTextFormat.PLAIN_TEXT
                    insertText = replaceText
                    textEdit = if (hasWordToReplace) {
                        EditorTextEdit(range = replaceRange, newText = replaceText)
                    } else {
                        null
                    }
                    additionalEdits = baseAdditionalEdits
                }

                val score = scoreCompletion(item, prefix)
                val serverSortKey = item.sortText ?: item.label
                val compositeSortKey = "${score}_$serverSortKey"

                CompletionItem(
                    label = item.label,
                    detail = item.detail ?: "",
                    insertText = insertText,
                    insertTextFormat = format,
                    textEdit = textEdit,
                    additionalTextEdits = additionalEdits,
                    filterText = item.filterText,
                    sortKey = compositeSortKey,
                    kind = kind,
                )
            }.sortedBy { (label, sortKey) -> sortKey ?: label }

            val _ = receiver.accept(CompletionResult(items = sweetItems, isIncomplete = false))
        }
    }

    private fun mapCompletionItemKind(kind: CompletionItemKind?): SweetCompletionItemKind = when (kind) {
        CompletionItemKind.Keyword -> SweetCompletionItemKind.KEYWORD
        CompletionItemKind.Function,
        CompletionItemKind.Method,
        CompletionItemKind.Constructor -> SweetCompletionItemKind.FUNCTION
        CompletionItemKind.Variable,
        CompletionItemKind.Field,
        CompletionItemKind.Constant,
        CompletionItemKind.Value -> SweetCompletionItemKind.VARIABLE
        CompletionItemKind.Class,
        CompletionItemKind.Enum,
        CompletionItemKind.Struct -> SweetCompletionItemKind.CLASS
        CompletionItemKind.Interface -> SweetCompletionItemKind.INTERFACE
        CompletionItemKind.Module -> SweetCompletionItemKind.MODULE
        CompletionItemKind.Property -> SweetCompletionItemKind.PROPERTY
        CompletionItemKind.Snippet -> SweetCompletionItemKind.SNIPPET
        else -> SweetCompletionItemKind.TEXT
    }
}
