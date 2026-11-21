package com.klyx.editor

import io.github.lumkit.sweeteditor.EditorTextEdit
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange

internal enum class CursorDirection { Left, Right, Up, Down }

internal fun TextRange.inDocumentOrder(): TextRange {
    return if (start.line > end.line || (start.line == end.line && start.column > end.column)) {
        TextRange(end, start)
    } else {
        this
    }
}

internal fun SweetEditorController.moveCursor(direction: CursorDirection, byWord: Boolean = false) {
    val cursor = getCursorPosition() ?: return
    val selection = getSelection()?.inDocumentOrder()
    val destination = if (selection != null && direction == CursorDirection.Left) {
        selection.start
    } else if (selection != null && direction == CursorDirection.Right) {
        selection.end
    } else {
        val lines = getDocument().text.lines()
        val line = lines.getOrNull(cursor.line) ?: return
        val column = cursor.column.coerceIn(0, line.length)
        when (direction) {
            CursorDirection.Left -> when {
                column > 0 -> cursor.copy(column = if (byWord) line.wordStart(column) else line.previousColumn(column))
                cursor.line > 0 -> TextPosition(cursor.line - 1, lines[cursor.line - 1].length)
                else -> cursor
            }

            CursorDirection.Right -> when {
                column < line.length -> cursor.copy(column = if (byWord) line.wordEnd(column) else line.nextColumn(column))

                cursor.line < lines.lastIndex -> TextPosition(cursor.line + 1, 0)
                else -> cursor
            }

            CursorDirection.Up, CursorDirection.Down -> {
                val step = if (direction == CursorDirection.Up) -1 else 1
                var target = cursor.line + step
                while (target in lines.indices && !isLineVisible(target)) target += step
                if (target in lines.indices) {
                    TextPosition(target, lines[target].safeColumn(column))
                } else cursor
            }
        }
    }
    setCursorPosition(destination)
    ensureCursorVisible()
}

internal fun SweetEditorController.insertTab() {
    if (isInlineSuggestionShowing()) {
        acceptInlineSuggestion()
    } else if (isInLinkedEditing()) {
        linkedEditingNext()
    } else if (getSelection() != null) {
        changeIndent(outdent = false)
    } else {
        val settings = getSettings()
        val column = getCursorPosition()?.column ?: return
        val tabSize = settings.tabSize.coerceAtLeast(1)
        val spaces = tabSize - column % tabSize
        insertText(if (settings.insertSpaces) " ".repeat(spaces) else "\t")
    }
}

internal fun SweetEditorController.changeIndent(outdent: Boolean) {
    val settings = getSettings()
    val indent = if (settings.insertSpaces) " ".repeat(settings.tabSize) else "\t"
    editLines { lines, selected ->
        selected.mapNotNull { lineNumber ->
            val line = lines[lineNumber]
            if (!outdent) return@mapNotNull lineEdit(lineNumber, 0, 0, indent)
            val count = if (line.startsWith('\t')) 1 else line.take(settings.tabSize).takeWhile { it == ' ' }.length
            if (count == 0) null else lineEdit(lineNumber, 0, count, "")
        }
    }
}

internal fun lineCommentPrefix(language: String): String? = when (language) {
    "c", "cpp", "csharp", "dart", "go", "java", "javascript", "kotlin", "rust", "swift", "typescript" -> "//"
    "dockerfile", "makefile", "python", "ruby", "shellscript", "toml", "yaml" -> "#"
    "lua", "sql" -> "--"
    else -> null
}

internal fun SweetEditorController.toggleLineComment(prefix: String) {
    editLines { lines, selected ->
        val nonBlank = selected.filter { lines[it].isNotBlank() }
        val uncomment = nonBlank.all { lines[it].trimStart().startsWith(prefix) }
        nonBlank.map { lineNumber ->
            val line = lines[lineNumber]
            val start = line.indexOfFirst { !it.isWhitespace() }
            if (uncomment) {
                val end = start + prefix.length
                lineEdit(lineNumber, start, end + if (line.getOrNull(end) == ' ') 1 else 0, "")
            } else {
                lineEdit(lineNumber, start, start, "$prefix ")
            }
        }
    }
}

private fun SweetEditorController.editLines(
    editsFor: (List<String>, IntRange) -> List<EditorTextEdit>,
) {
    val cursor = getCursorPosition() ?: return
    val selection = getSelection()
    val ordered = selection?.inDocumentOrder()
    val start = ordered?.start ?: cursor
    val end = ordered?.end ?: cursor
    val lastLine = if (end.line > start.line && end.column == 0) end.line - 1 else end.line
    val lines = getDocument().text.lines()
    if (start.line !in lines.indices || lastLine !in lines.indices) return
    val edits = editsFor(lines, start.line..lastLine)
    if (edits.isEmpty()) return

    val byLine = edits.associateBy { it.range.start.line }
    fun translate(position: TextPosition): TextPosition {
        val edit = byLine[position.line] ?: return position
        if (position.column < edit.range.start.column) return position
        val column = maxOf(0, position.column - edit.range.end.column)
        return position.copy(column = edit.range.start.column + edit.newText.length + column)
    }

    applyTextEdits(edits)
    if (selection == null) setCursorPosition(translate(cursor))
    else setSelection(translate(selection.start), translate(selection.end))
    ensureCursorVisible()
}

private fun lineEdit(line: Int, start: Int, end: Int, text: String) =
    EditorTextEdit(TextRange(TextPosition(line, start), TextPosition(line, end)), text)

private fun String.safeColumn(column: Int): Int {
    val clamped = column.coerceAtMost(length)
    return if (clamped > 0 && getOrNull(clamped)?.isLowSurrogate() == true && this[clamped - 1].isHighSurrogate()) {
        clamped - 1
    } else clamped
}

private fun String.previousColumn(column: Int): Int = safeColumn(column - 1)

private fun String.nextColumn(column: Int): Int =
    if (column + 1 < length && this[column].isHighSurrogate() && this[column + 1].isLowSurrogate()) column + 2
    else column + 1

private fun String.wordStart(column: Int): Int {
    var index = column
    while (index > 0 && !this[index - 1].isWordCharacter()) index--
    while (index > 0 && this[index - 1].isWordCharacter()) index--
    return safeColumn(index)
}

private fun String.wordEnd(column: Int): Int {
    var index = column
    while (index < length && this[index].isWordCharacter()) index++
    while (index < length && !this[index].isWordCharacter()) index++
    return safeColumn(index)
}

private fun Char.isWordCharacter() = isLetterOrDigit() || this == '_'
