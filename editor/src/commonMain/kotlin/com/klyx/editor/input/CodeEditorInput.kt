package com.klyx.editor.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.utf16CodePoint
import com.klyx.editor.CodeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.editor.cursor.CursorPosition
import com.klyx.editor.selection.Selection

@ExperimentalCodeEditorApi
internal fun CodeEditorState.handleKeyEvent(event: KeyEvent) {
    when (event.key) {
        Key.Backspace -> {
            if (buffer.isEmpty()) return
            val selectionRange = getResolvedSelectionRange()

            if (selectionRange.collapsed) {
                if (cursorPosition.offset == 0) return

                deleteAt(cursorPosition.offset - 1)
            } else {
                deleteRange(selectionRange)
                clearSelection()
            }
        }

        Key.DirectionRight -> {
            if (event.isShiftPressed) {
                if (selectionAnchor == null) selectionAnchor = cursorPosition.offset
                val newCursor = (cursorPosition.offset + 1).coerceAtMost(buffer.length)
                cursorPosition = CursorPosition(newCursor)
                selection = Selection(
                    start = Selection.AnchorInfo(direction, selectionAnchor!!, selectableId),
                    end = Selection.AnchorInfo(direction, newCursor, selectableId),
                    handlesCrossed = newCursor < selectionAnchor!!
                )
            } else {
                val newCursor = (cursorPosition.offset + 1).coerceAtMost(buffer.length)
                cursorPosition = CursorPosition(newCursor)
                clearSelection()
            }
        }

        Key.DirectionLeft -> {
            if (event.isShiftPressed) {
                if (selectionAnchor == null) selectionAnchor = cursorPosition.offset
                val newCursor = (cursorPosition.offset - 1).coerceAtLeast(0)
                cursorPosition = CursorPosition(newCursor)
                selection = Selection(
                    start = Selection.AnchorInfo(direction, selectionAnchor!!, selectableId),
                    end = Selection.AnchorInfo(direction, newCursor, selectableId),
                    handlesCrossed = newCursor < selectionAnchor!!
                )
            } else {
                val newCursor = (cursorPosition.offset - 1).coerceAtLeast(0)
                cursorPosition = CursorPosition(newCursor)
                clearSelection()
            }
        }

        else -> {
            if (event.utf16CodePoint != 0) {
                val char = event.utf16CodePoint.toChar()
                if (char.isDefined() && !char.isISOControl()) {
                    val selectionRange = getResolvedSelectionRange()

                    if (selectionRange.collapsed) {
                        insert(char.toString())
                    } else {
                        deleteRange(selectionRange)
                        clearSelection()
                        insert(char.toString())
                    }
                }
            }
        }
    }
}
