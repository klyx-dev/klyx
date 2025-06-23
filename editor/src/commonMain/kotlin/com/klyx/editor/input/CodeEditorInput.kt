package com.klyx.editor.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.utf16CodePoint
import com.klyx.editor.CodeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.editor.cursor.CursorPosition
import com.klyx.editor.selection.Selection
import com.klyx.lineSeparator

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
            val newCursor = (cursorPosition.offset + 1).coerceAtMost(buffer.length)

            if (event.isShiftPressed) {
                if (selectionAnchor == null) selectionAnchor = cursorPosition.offset

                selection = Selection(
                    start = Selection.AnchorInfo(direction, selectionAnchor!!, selectableId),
                    end = Selection.AnchorInfo(direction, newCursor, selectableId),
                    handlesCrossed = newCursor < selectionAnchor!!
                )
            } else {
                clearSelection()
            }

            cursorPosition = CursorPosition(newCursor)
        }

        Key.DirectionLeft -> {
            val newCursor = (cursorPosition.offset - 1).coerceAtLeast(0)

            if (event.isShiftPressed) {
                if (selectionAnchor == null) selectionAnchor = cursorPosition.offset

                selection = Selection(
                    start = Selection.AnchorInfo(direction, selectionAnchor!!, selectableId),
                    end = Selection.AnchorInfo(direction, newCursor, selectableId),
                    handlesCrossed = newCursor < selectionAnchor!!
                )
            } else {
                clearSelection()
            }

            cursorPosition = CursorPosition(newCursor)
        }

        Key.DirectionUp -> {
            val line = cursorPosition.line

            val prevLine = (line - 1).coerceAtLeast(0)
            val prevLineStart = getLineStart(prevLine)

            val newCursor = (prevLineStart + cursorPosition.column).coerceAtMost(getLineEnd(prevLine) - 1)

            if (event.isShiftPressed) {
                if (selectionAnchor == null) selectionAnchor = cursorPosition.offset

                selection = Selection(
                    start = Selection.AnchorInfo(direction, selectionAnchor!!, selectableId),
                    end = Selection.AnchorInfo(direction, newCursor, selectableId),
                    handlesCrossed = newCursor < selectionAnchor!!
                )
            } else {
                clearSelection()
            }

            cursorPosition = CursorPosition(newCursor)
        }

        Key.DirectionDown -> {
            val line = cursorPosition.line

            val nextLine = (line + 1).coerceAtMost(lineCount - 1)
            val nextLineStart = getLineStart(nextLine)

            val newCursor = (nextLineStart + cursorPosition.column).coerceAtMost(getLineEnd(nextLine) - 1)

            if (event.isShiftPressed) {
                if (selectionAnchor == null) selectionAnchor = cursorPosition.offset

                selection = Selection(
                    start = Selection.AnchorInfo(direction, selectionAnchor!!, selectableId),
                    end = Selection.AnchorInfo(direction, newCursor, selectableId),
                    handlesCrossed = newCursor < selectionAnchor!!
                )
            } else {
                clearSelection()
            }

            cursorPosition = CursorPosition(newCursor)
        }

        Key.Enter -> insert(lineSeparator)

        else -> {
            if (event.isCtrlPressed) {
                when (event.key) {
                    Key.V -> paste()
                    Key.C -> copy()
                    Key.X -> cut()
                    Key.A -> selectAll()
                }
            }

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
