package com.klyx.editor

import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.klyx.editor.cursor.CursorPosition
import com.klyx.editor.cursor.TextSelection
import com.klyx.editor.language.KlyxLanguage
import com.klyx.editor.language.parse
import com.klyx.editor.theme.KlyxColorScheme
import io.github.treesitter.ktreesitter.Parser

@Composable
fun rememberCodeEditorState(initialText: String? = null): CodeEditorState {
    return remember(initialText) { CodeEditorState(initialText = initialText ?: "") }
}

@Stable
class CodeEditorState(
    val initialText: String = "",
) {
    private val buffer = StringBuilder(initialText) // I will use Rope like data structure later

    @set:JvmName("setTextInternal")
    var text by mutableStateOf(buffer.toString())
        private set

    val totalLines get() = buffer.lines().size

    var cursorPosition by mutableStateOf(CursorPosition.Start)
    var selection by mutableStateOf(TextSelection.Start)
    var scrollOffset by mutableStateOf(Offset.Zero)

    internal var viewportSize = IntSize.Zero
    internal var lineHeight = 0f
    internal var lineHeightWithSpacing = 0f
    internal var gutterWidth = 0f
    internal var gutterPadding = 0f
    internal var measureText: (String) -> Float = { 0f }

    internal lateinit var language: KlyxLanguage
    internal lateinit var colorScheme: KlyxColorScheme

    internal val visibleLines: List<Pair<Int, String>>
        get() {
            if (lineHeightWithSpacing <= 0 ||
                viewportSize.height <= 0 ||
                !::language.isInitialized ||
                !::colorScheme.isInitialized
            ) return emptyList()

            val lines = text.lines()
            val totalLines = lines.size

            val first = (-scrollOffset.y / lineHeightWithSpacing).toInt().coerceIn(0, totalLines - 1)
            val last = ((-scrollOffset.y + viewportSize.height) / lineHeightWithSpacing).toInt().coerceIn(first, totalLines - 1)

            return (first .. last).map { it to lines[it] }
        }

    private fun updateText() {
        text = buffer.toString()
    }

    fun insert(offset: Int, text: String) {
        buffer.insert(offset, text)
        updateText()
    }

    fun insert(position: CursorPosition, text: String) {
        val line = position.line.coerceIn(1, totalLines)
        val column = position.column.coerceIn(0, getLine(line - 1)?.length ?: 0)
        buffer.insert(buffer.lines().take(line - 1).sumOf { it.length + 1 } + column, text)
        if (text.contains("\n")) {
            val lines = text.lines()
            val lastLine = lines.last()
            moveCursorTo(CursorPosition(line + lines.size - 1, lastLine.length))
        } else {
            moveCursorTo(CursorPosition(line, column + text.length))
        }
        updateText()
    }

    fun insertAtCursor(text: String) = insert(cursorPosition, text)

    fun insert(text: String) {
        buffer.append(text)
        updateText()
    }

    fun insertLine(text: String) {
        buffer.appendLine(text)
        updateText()
    }

    fun delete(offset: Int, length: Int) {
        buffer.delete(offset, offset + length)
        updateText()
    }

    fun delete(position: CursorPosition, length: Int) {
        val line = position.line.coerceIn(1, totalLines)
        val column = position.column.coerceIn(0, getLine(line - 1)?.length ?: 0)

        if (column == 0 && line > 1) {
            // delete newline character from previous line
            val prevLine = getLine(line - 2) ?: ""
            val offset = buffer.lines().take(line - 2).sumOf { it.length + 1 } + prevLine.length
            buffer.delete(offset, offset + 1)
            moveCursorTo(CursorPosition(line - 1, prevLine.length))
        } else {
            val offset = buffer.lines().take(line - 1).sumOf { it.length + 1 } + column
            if (offset > 0) {
                buffer.delete((offset - length).coerceAtLeast(0), offset)
                moveCursorTo(CursorPosition(line, (column - length).coerceAtLeast(0)))
            }
        }
        updateText()
    }

    fun deleteAtCursor(length: Int) = delete(cursorPosition, length)

    fun replace(offset: Int, length: Int, text: String) {
        buffer.replace(offset, offset + length, text)
        updateText()
    }

    fun setText(newText: String) {
        buffer.clear()
        buffer.append(newText)
        updateText()
    }

    fun getLine(index: Int): String? {
        return buffer.lines().getOrNull(index)
    }

    fun moveCursorTo(position: CursorPosition) {
        val line = position.line.coerceIn(1, totalLines)
        val currentLine = getLine(line - 1) ?: ""
        val column = position.column.coerceIn(0, currentLine.length)
        cursorPosition = CursorPosition(line, column)
        ensureCursorInView()
    }

    fun moveCursorToEnd() {
        val line = totalLines
        val currentLine = getLine(line - 1) ?: ""
        val column = currentLine.length
        cursorPosition = CursorPosition(line, column)
        ensureCursorInView()
    }

    fun moveCursorAtEndOfLine() {
        val line = cursorPosition.line
        val currentLine = getLine(line - 1) ?: ""
        val column = currentLine.length
        cursorPosition = CursorPosition(line, column)
        ensureCursorInView()
    }

    fun moveCursorAtStartOfLine() {
        cursorPosition = cursorPosition.copy(column = 0)
        ensureCursorInView()
    }

    fun moveCursorToStart() {
        cursorPosition = CursorPosition.Start
        ensureCursorInView()
    }

    fun moveCursor(direction: CursorPosition.Direction, @IntRange(from = 1) length: Int = 1) {
        val currentLine = getLine(cursorPosition.line - 1) ?: ""
        val newPosition = cursorPosition.move(direction, totalLines, currentLine.length, length)
        moveCursorTo(newPosition)
    }

    private fun ensureCursorInView() {
        val line = cursorPosition.line - 1
        val lineText = getLine(line) ?: return
        val column = cursorPosition.column.coerceIn(0, lineText.length)

        val cursorY = line * lineHeightWithSpacing
        val cursorBottom = cursorY + lineHeightWithSpacing

        val cursorX = measureText(lineText.substring(0, column))
        val cursorRight = gutterWidth + gutterPadding + cursorX

        val padding = lineHeightWithSpacing * 2
        val maxScrollY = (totalLines * lineHeightWithSpacing - viewportSize.height + 200f).coerceAtLeast(0f)

        // vertical scroll
        if (cursorY < -scrollOffset.y + padding) {
            // scroll up
            scrollOffset = scrollOffset.copy(y = -(cursorY - padding).coerceAtMost(maxScrollY).coerceAtLeast(0f))
        } else if (cursorBottom > -scrollOffset.y + viewportSize.height - padding) {
            // scroll down
            val newY = -(cursorBottom - viewportSize.height + padding)
            scrollOffset = scrollOffset.copy(y = newY.coerceAtLeast(-maxScrollY))
        }

        // horizontal scroll
        val maxScrollX = visibleLines.maxOfOrNull { (_, line) ->
            measureText(line)
        }?.let { it + gutterWidth + gutterPadding * 2 } ?: 0f
        val maxScroll = (maxScrollX - viewportSize.width + 200f).coerceAtLeast(0f)

        if (cursorRight < -scrollOffset.x + padding) {
            // scroll left
            scrollOffset = scrollOffset.copy(x = -(cursorRight - padding).coerceAtMost(maxScroll).coerceAtLeast(0f))
        } else if (cursorRight > -scrollOffset.x + viewportSize.width - padding) {
            // scroll right
            val newX = -(cursorRight - viewportSize.width + padding)
            scrollOffset = scrollOffset.copy(x = newX.coerceAtLeast(-maxScroll))
        }
    }
}
