package com.klyx.editor

import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.substring
import com.klyx.core.FpsTracker
import com.klyx.core.event.EventBus
import com.klyx.editor.clipboard.clipEntryOf
import com.klyx.editor.clipboard.paste
import com.klyx.editor.cursor.CursorPosition
import com.klyx.editor.input.handleKeyEvent
import com.klyx.editor.selection.Selection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@Stable
@ExperimentalCodeEditorApi
class CodeEditorState(
    initialText: String = "",
    private val clipboard: Clipboard,
    private val textToolbar: TextToolbar,
    private val coroutineScope: CoroutineScope
) {
    internal val buffer = StringBuilder(initialText)

    private var _text by mutableStateOf(initialText)
    val text get() = _text

    var cursorPosition by mutableStateOf(CursorPosition.Initial)

    val selectionRange: TextRange
        get() = selection?.toTextRange() ?: TextRange.Zero

    internal var selection: Selection? by mutableStateOf(null)
    internal val selectableId = 0L
    internal val direction = ResolvedTextDirection.Ltr
    internal var selectionAnchor: Int? = null

    internal lateinit var textLayoutResult: TextLayoutResult

    private val fpsTracker = FpsTracker()
    val fps = fpsTracker.fps

    val lineCount get() = textLayoutResult.lineCount

    init {
        cursorPosition = CursorPosition(buffer.length)

        EventBus.instance.subscribe<KeyEvent> { event ->
            if (event.type == KeyEventType.KeyDown) {
                handleKeyEvent(event)
            }
        }
    }

    @Stable
    internal suspend fun startFpsTracker() = fpsTracker.start()

    internal fun showTextToolbar(position: Offset) {
        coroutineScope.launch(Dispatchers.Main) {
            textToolbar.showMenu(
                rect = Rect(
                    offset = position,
                    size = Size.VisibilityThreshold
                ),
                onCutRequested = ::cut,
                onPasteRequested = ::paste,
                onCopyRequested = ::copy
            )
        }
    }

    internal fun hideTextToolbarIfShown() {
        coroutineScope.launch(Dispatchers.Main) {
            if (textToolbar.status == TextToolbarStatus.Shown) {
                textToolbar.hide()
            }
        }
    }

    private fun moveCursor(offset: Int) {
        cursorPosition = cursorPosition.copy(
            offset = (cursorPosition.offset + offset).coerceIn(0, buffer.length)
        )
    }

    private fun updateText() {
        _text = buffer.toString()
    }

    fun setText(newText: String) {
        buffer.clear()
        buffer.append(newText)
        cursorPosition = CursorPosition(newText.length)
        updateText()
    }

    fun insert(
        text: String,
        position: CursorPosition = cursorPosition
    ) {
        buffer.insert(position.offset, text)
        updateText()
        moveCursor(text.length)
    }

    fun deleteAt(offset: Int) {
        buffer.deleteAt(offset)
        moveCursor(-1)
        updateText()
    }

    fun deleteRange(startIndex: Int, endIndex: Int) {
        buffer.deleteRange(startIndex, endIndex)
        if (cursorPosition.offset != startIndex) {
            moveCursor(-(endIndex - startIndex))
        }
        updateText()
    }

    fun deleteRange(range: TextRange) {
        deleteRange(range.start, range.end)
    }

    fun deleteSelected() {
        clearSelection()
        deleteRange(getResolvedSelectionRange())
    }

    fun deleteRange(range: IntRange) {
        deleteRange(range.first, range.last)
    }

    fun select(range: TextRange) {
        selection = Selection(
            start = Selection.AnchorInfo(direction, range.start, selectableId),
            end = Selection.AnchorInfo(direction, range.end, selectableId),
            handlesCrossed = range.end < range.start
        )
        selectionAnchor = range.start
    }

    fun clearSelection() {
        selection = null
        selectionAnchor = null
        hideTextToolbarIfShown()
    }

    fun copy(): String? {
        val selectionRange = getResolvedSelectionRange()

        if (selectionRange.collapsed) return null
        val str = text.substring(selectionRange)

        coroutineScope.launch {
            clipboard.setClipEntry(clipEntryOf(str))
        }
        return str
    }

    fun paste() {
        coroutineScope.launch {
            clipboard.paste()?.let {
                if (it.isEmpty()) return@launch

                val selectionRange = getResolvedSelectionRange()

                if (!selectionRange.collapsed) {
                    buffer.setRange(
                        startIndex = selectionRange.start,
                        endIndex = selectionRange.end,
                        value = it.toString()
                    )
                    cursorPosition = CursorPosition(selectionRange.start + it.length)
                    clearSelection()
                    updateText()
                } else {
                    insert(it.toString())
                }
            }
        }
    }

    fun cut(): String? {
        val selectionRange = getResolvedSelectionRange()

        if (selectionRange.collapsed) return null
        val str = text.substring(selectionRange)

        coroutineScope.launch {
            clipboard.setClipEntry(clipEntryOf(str))
            deleteRange(selectionRange)
            clearSelection()
        }

        return str
    }

    internal fun getCursorRect(offset: Int = cursorPosition.offset) = textLayoutResult.getCursorRect(offset.coerceAtMost(text.length))
    internal fun getPathForRange(start: Int, end: Int) = textLayoutResult.getPathForRange(start, end)
    internal fun getPathForRange(range: TextRange) = textLayoutResult.getPathForRange(range.start, range.end)
    internal fun getPathForSelectionRange() = getPathForRange(getResolvedSelectionRange())
    internal fun getOffsetForPosition(position: Offset) = textLayoutResult.getOffsetForPosition(position)
    internal fun getLineHeight(lineIndex: Int) = textLayoutResult.getLineBottom(lineIndex) - textLayoutResult.getLineTop(lineIndex)

    /**
     * Returns the text range of the word at the given character offset.
     *
     * Characters not part of a word, such as spaces, symbols, and punctuation, have word breaks on
     * both sides. In such cases, this method will return a text range that contains the given
     * character offset.
     *
     * Word boundaries are defined more precisely in Unicode Standard Annex #29
     * <http://www.unicode.org/reports/tr29/#Word_Boundaries>.
     */
    fun getWordBoundary(offset: Int) = textLayoutResult.getWordBoundary(offset)
    fun getCurrentWordBoundary() = getWordBoundary(cursorPosition.offset)

    /**
     * Returns the bounding box of the character for given character offset.
     */
    fun getBoundingBox(offset: Int) = textLayoutResult.getBoundingBox(offset)

    /**
     * Returns the line number on which the specified text offset appears.
     *
     * If you ask for a position before 0, you get 0; if you ask for a position beyond the end of
     * the text, you get the last line.
     *
     * @param offset a character offset
     * @return the 0 origin line number.
     */
    fun getLineForOffset(offset: Int) = textLayoutResult.getLineForOffset(offset)

    /**
     * Returns the start offset of the given line, inclusive.
     *
     * The start offset represents a position in text before the first character in the given line.
     * For example, `getLineStart(1)` will return 4 for the text below
     * <pre>
     * ┌────┐
     * │abcd│
     * │efg │
     * └────┘
     * </pre>
     *
     * @param lineIndex the line number
     * @return the start offset of the line
     */
    fun getLineStart(lineIndex: Int) = textLayoutResult.getLineStart(lineIndex)

    /**
     * Returns the end offset of the given line.
     *
     * The end offset represents a position in text after the last character in the given line. For
     * example, `getLineEnd(0)` will return 4 for the text below
     * <pre>
     * ┌────┐
     * │abcd│
     * │efg │
     * └────┘
     * </pre>
     *
     * Characters being ellipsized are treated as invisible characters. So that if visibleEnd is
     * false, it will return line end including the ellipsized characters and vice versa.
     *
     * @param lineIndex the line number
     * @param visibleEnd if true, the returned line end will not count trailing whitespaces or
     *   linefeed characters. Otherwise, this function will return the logical line end. By default
     *   it's false.
     * @return an exclusive end offset of the line.
     */
    fun getLineEnd(lineIndex: Int, visibleEnd: Boolean = false) = textLayoutResult.getLineEnd(lineIndex, visibleEnd)

    /**
     * Returns a TextRange with start <= end, regardless of selection direction or handlesCrossed.
     * If there is no selection, returns a collapsed range at the cursor position.
     */
    fun getResolvedSelectionRange(): TextRange {
        if (selection == null) return TextRange.Zero

        val (selStart, selEnd) = if (selection!!.handlesCrossed) {
            val range = selection!!.toTextRange()
            range.end to range.start
        } else {
            selectionRange.start to selectionRange.end
        }

        return TextRange(minOf(selStart, selEnd), maxOf(selStart, selEnd))
    }

    fun isTextSelected() = !getResolvedSelectionRange().collapsed

    val CursorPosition.line get() = getLineForOffset(offset)
    val CursorPosition.column get() = offset - getLineStart(line)
}

@ExperimentalCodeEditorApi
@Composable
fun rememberCodeEditorState(
    initialText: String = "",
): CodeEditorState {
    val clipboard = LocalClipboard.current
    val textToolbar = LocalTextToolbar.current
    val scope = rememberCoroutineScope()

    return remember(initialText, clipboard) {
        CodeEditorState(
            initialText = initialText,
            clipboard = clipboard,
            textToolbar = textToolbar,
            coroutineScope = scope
        )
    }
}
