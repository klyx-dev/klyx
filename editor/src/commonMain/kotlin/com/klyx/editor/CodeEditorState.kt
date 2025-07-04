package com.klyx.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.substring
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import com.klyx.editor.clipboard.clipEntryOf
import com.klyx.editor.clipboard.paste
import com.klyx.editor.cursor.CursorPosition
import com.klyx.editor.selection.Selection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

@Stable
@ExperimentalCodeEditorApi
class CodeEditorState(
    initialText: String = ""
) {
    internal val buffer = StringBuilder(initialText)
    internal lateinit var clipboard: Clipboard
    internal lateinit var textToolbar: TextToolbar
    internal lateinit var coroutineScope: CoroutineScope

    private var _text by mutableStateOf(initialText)
    val text get() = _text

    var cursorPosition by mutableStateOf(CursorPosition.Initial)

    val selectionRange: TextRange
        get() = selection?.toTextRange() ?: TextRange.Zero

    internal var selection: Selection? by mutableStateOf(null)
    internal val selectableId = 0L
    internal val direction = ResolvedTextDirection.Ltr
    internal var selectionAnchor: Int? = null

    internal var textLayoutResult: TextLayoutResult? = null

    internal var scrollX by mutableFloatStateOf(0f)
    internal var scrollY by mutableFloatStateOf(0f)
    internal val scrollOffset get() = Offset(scrollX, scrollY)
    internal var canvasSize = Size.Zero
    internal val viewport get() = Rect(-scrollOffset, canvasSize)

    val lineCount get() = textLayoutResult!!.lineCount

    private val textChangedListeners = mutableSetOf<(String) -> Unit>()

    init {
        cursorPosition = CursorPosition(buffer.length)
    }

    internal fun showTextToolbar(editable: Boolean = true) {
        coroutineScope.launch(Dispatchers.Main) {
            val path = getPathForSelectionRange()

            textToolbar.showMenu(
                rect = path.getBounds().apply { translate(scrollOffset) },
                onCutRequested = if (editable) {
                    { cut() }
                } else null,
                onPasteRequested = if (editable) {
                    { paste() }
                } else null,
                onCopyRequested = ::copy,
                onSelectAllRequested = ::selectAll
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

    private fun coerceScrollOffset() {
        if (textLayoutResult == null) return
        val result = textLayoutResult!!
        val height = result.size.height.toFloat()
        val width = result.size.width.toFloat()

        scrollX = scrollX.fastCoerceIn(
            maximumValue = width - 50f.fastCoerceAtMost(width),
            minimumValue = 0f
        )

        val extraBottomPadding = getLineHeight((result.lineCount - 1).fastCoerceAtLeast(0)) * 4

        scrollY = scrollY.fastCoerceIn(
            maximumValue = height - extraBottomPadding.fastCoerceAtMost(height),
            minimumValue = 0f
        )
    }

    internal fun scroll(amount: Offset) {
        scrollX += amount.x
        scrollY += amount.y
        coerceScrollOffset()
    }

    internal fun scrollTo(position: Offset) {
        scrollX = position.x
        scrollY = position.y
        coerceScrollOffset()
    }

    internal fun ensureCursorInView() {
        if (textLayoutResult == null) return

        val cursorRect = getCursorRect()
        val padding = 100f

        var newScrollX = scrollX
        var newScrollY = scrollY

        // adjust horizontal scroll
        if (cursorRect.left < viewport.left + padding) {
            newScrollX = cursorRect.left - padding
        } else if (cursorRect.right > viewport.right - padding) {
            newScrollX = cursorRect.right - viewport.width + padding
        }

        // adjust vertical scroll
        if (cursorRect.top < viewport.top + padding) {
            newScrollY = cursorRect.top - padding
        } else if (cursorRect.bottom > viewport.bottom - padding) {
            newScrollY = cursorRect.bottom - viewport.height + padding
        }

        if (newScrollX != scrollX || newScrollY != scrollY) {
            scrollTo(Offset(newScrollX, newScrollY))
        }
    }

    private fun moveCursor(offset: Int) {
        ensureCursorInView()
        cursorPosition = cursorPosition.copy(
            offset = (cursorPosition.offset + offset).coerceIn(0, buffer.length)
        )
    }

    fun addTextChangedListener(listener: (String) -> Unit) {
        textChangedListeners.add(listener)
    }

    fun removeTextChangedListener(listener: (String) -> Unit) {
        textChangedListeners.remove(listener)
    }

    private fun notifyTextChanged(newText: String) {
        for (listener in textChangedListeners) {
            listener(newText)
        }
    }

    private fun updateText() {
        _text = buffer.toString()
        notifyTextChanged(_text)
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
        deleteRange(getResolvedSelectionRange())
        clearSelection()
    }

    fun deleteRange(range: IntRange) {
        deleteRange(range.first, range.last)
    }

    fun deleteAroundCursor(before: Int, after: Int) {
        val start = (cursorPosition.offset - before).coerceAtLeast(0)
        val end = (cursorPosition.offset + after).coerceAtMost(buffer.length)
        deleteRange(start, end)
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

    fun selectAll() {
        select(TextRange(0, buffer.length))
    }

    internal fun getCursorRect(offset: Int = cursorPosition.offset) = runCatching {
        textLayoutResult!!.getCursorRect(offset.coerceAtMost(buffer.length))
    }.getOrElse { Rect.Zero }

    internal fun getPathForRange(start: Int, end: Int) = textLayoutResult!!.getPathForRange(start, end)
    internal fun getPathForRange(range: TextRange) = textLayoutResult!!.getPathForRange(range.start, range.end)
    internal fun getPathForSelectionRange() = getPathForRange(getResolvedSelectionRange())
    internal fun getOffsetForPosition(position: Offset) = textLayoutResult!!.getOffsetForPosition(position)
    internal fun getLineHeight(lineIndex: Int) = textLayoutResult!!.multiParagraph.getLineHeight(lineIndex)
    internal fun getLineTop(lineIndex: Int) = textLayoutResult!!.getLineTop(lineIndex)
    internal fun getLineBottom(lineIndex: Int) = textLayoutResult!!.getLineBottom(lineIndex)

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
    fun getWordBoundary(offset: Int) = textLayoutResult!!.getWordBoundary(offset)
    fun getCurrentWordBoundary() = getWordBoundary(cursorPosition.offset)

    /**
     * Returns the bounding box of the character for given character offset.
     */
    fun getBoundingBox(offset: Int) = textLayoutResult!!.getBoundingBox(offset)

    /**
     * Returns the line number on which the specified text offset appears.
     *
     * If you ask for a position before 0, you get 0; if you ask for a position beyond the end of
     * the text, you get the last line.
     *
     * @param offset a character offset
     * @return the 0 origin line number.
     */
    fun getLineForOffset(offset: Int) = textLayoutResult!!.getLineForOffset(offset)

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
    fun getLineStart(lineIndex: Int) = textLayoutResult!!.getLineStart(lineIndex)

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
    fun getLineEnd(lineIndex: Int, visibleEnd: Boolean = false) = textLayoutResult!!.getLineEnd(lineIndex, visibleEnd)

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

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = _text
    operator fun setValue(thisRef: Any?, property: KProperty<*>, text: String) = setText(text)
}

@ExperimentalCodeEditorApi
@Composable
fun rememberCodeEditorState(
    initialText: String = "",
): CodeEditorState {
    return remember(initialText) {
        CodeEditorState(initialText = initialText)
    }
}

@ExperimentalCodeEditorApi
fun CodeEditorState(other: CodeEditorState) = CodeEditorState(initialText = other.buffer.toString()).apply {
    clipboard = other.clipboard
    textToolbar = other.textToolbar
    coroutineScope = other.coroutineScope
}
