package com.klyx.lsp

import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.lang.completion.snippet.CodeSnippet
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditorDelegate

/**
 * A completion item backed by an LSP [com.klyx.lsp.CompletionItem].
 */
class LspCompletionItem(
    label: CharSequence,
    desc: CharSequence,
    prefixLength: Int,
    commitText: String,
    /** LSP `textEdit` replacement range start (null when the server didn't provide one). */
    private val replaceStartLine: Int? = null,
    private val replaceStartColumn: Int? = null,
    /** LSP `textEdit` replacement range end (null when the server didn't provide one). */
    private val replaceEndLine: Int? = null,
    private val replaceEndColumn: Int? = null,
    /** Parsed LSP snippet; when non-null, insertion uses the snippet controller. */
    private val snippet: CodeSnippet? = null,
) : SimpleCompletionItem(label, desc, prefixLength, commitText) {

    /** Mirrors the LSP `preselect` field; such items are sorted first. */
    var preselect: Boolean = false

    override fun performCompletion(
        editor: CodeEditorDelegate,
        text: Content,
        position: CharPosition
    ) {
        if (snippet != null) {
            performSnippetCompletion(editor, text, position)
        } else {
            performCompletion(editor, text, position.line, position.column)
        }
    }

    override fun performCompletion(
        editor: CodeEditorDelegate,
        text: Content,
        line: Int,
        column: Int
    ) {
        val commitText = this.commitText ?: return

        val actualPrefix = prefixLength.coerceAtMost(column)

        var startLine = replaceStartLine?.coerceIn(0, text.lineCount - 1) ?: line
        var endLine = replaceEndLine?.coerceIn(0, text.lineCount - 1) ?: line
        var startColumn = (replaceStartColumn ?: (column - actualPrefix))
            .coerceIn(0, text.getColumnCount(startLine))
        var endColumn = (replaceEndColumn ?: column).coerceIn(0, text.getColumnCount(endLine))

        if (startLine > endLine || (startLine == endLine && startColumn > endColumn)) {
            val tmpLine = startLine; startLine = endLine; endLine = tmpLine
            val tmpCol = startColumn; startColumn = endColumn; endColumn = tmpCol
        }

        if (startLine == endLine && startColumn == endColumn) {
            text.insert(startLine, startColumn, commitText)
        } else {
            text.replace(startLine, startColumn, endLine, endColumn, commitText)
        }
    }

    private fun performSnippetCompletion(
        editor: CodeEditorDelegate,
        text: Content,
        position: CharPosition
    ) {
        val snippet = this.snippet ?: return

        val actualPrefix = prefixLength.coerceAtMost(position.column)
        val cursorIndex = if (position.index >= 0) {
            position.index
        } else {
            text.getIndexer().getCharIndex(position.line, position.column)
        }
        val startIndex = replaceStartLine?.let { line ->
            replaceStartColumn?.let { column -> text.getIndexer().getCharIndex(line, column) }
        } ?: (cursorIndex - actualPrefix)
        val safeStart = startIndex.coerceIn(0, cursorIndex)

        val selectedText = if (safeStart < cursorIndex && cursorIndex <= text.length) {
            text.subSequence(safeStart, cursorIndex).toString()
        } else {
            ""
        }

        text.delete(safeStart, cursorIndex)
        editor.snippetController.startSnippet(safeStart, snippet, selectedText)
    }
}
