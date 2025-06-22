package com.klyx.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.substring
import com.klyx.core.event.EventBus
import com.klyx.editor.clipboard.clipEntryOf
import com.klyx.editor.clipboard.paste
import com.klyx.editor.cursor.CursorPosition
import com.klyx.editor.input.handleKeyEvent
import com.klyx.editor.selection.Selection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
@ExperimentalCodeEditorApi
class CodeEditorState(
    initialText: String = "",
    private val clipboard: Clipboard,
    private val coroutineScope: CoroutineScope
) {
    internal val buffer = StringBuilder(initialText)

    private var _text by mutableStateOf(initialText)
    val text get() = _text

    var cursorPosition by mutableStateOf(CursorPosition.Initial)

    val selectionRange: TextRange
        get() = selection?.toTextRange() ?: TextRange(cursorPosition.offset, cursorPosition.offset)

    internal var selection: Selection? by mutableStateOf(null)
    internal val selectableId = 0L
    internal val direction = ResolvedTextDirection.Ltr
    internal var selectionAnchor: Int? = null

    init {
        cursorPosition = CursorPosition(buffer.length)

        EventBus.instance.subscribe<KeyEvent> { event ->
            println(event)

            if (event.type == KeyEventType.KeyDown) {
                handleKeyEvent(event)
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
        moveCursor(-(endIndex - startIndex))
        updateText()
    }

    fun deleteRange(range: TextRange) {
        deleteRange(range.start, range.end)
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
    }

    fun copyText() {
        val selectionRange = getResolvedSelectionRange()

        if (selectionRange.collapsed) return
        coroutineScope.launch {
            clipboard.setClipEntry(clipEntryOf(text.substring(selectionRange)))
        }
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
}

@ExperimentalCodeEditorApi
@Composable
fun rememberCodeEditorState(
    initialText: String = "",
): CodeEditorState {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    return remember(initialText, clipboard) {
        CodeEditorState(
            initialText = initialText,
            clipboard = clipboard,
            coroutineScope = scope
        )
    }
}
