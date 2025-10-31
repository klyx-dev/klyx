package com.klyx.editor.compose.input

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.Direction
import com.klyx.editor.compose.moveCursorAtEndOfLine
import com.klyx.editor.compose.moveCursorAtStartOfLine
import com.klyx.editor.compose.text.KeyCommand
import com.klyx.editor.compose.text.platformDefaultKeyMapping
import com.klyx.lineSeparator

internal fun CodeEditorState.handleKeyEvent(event: KeyEvent) {
    if (event.type != KeyEventType.KeyDown) return

    when (platformDefaultKeyMapping.map(event)) {
        KeyCommand.UNDO -> if (canUndo()) undo()
        KeyCommand.REDO -> if (canRedo()) redo()
        KeyCommand.COPY -> {}
        KeyCommand.LEFT_CHAR -> moveCursor(Direction.Left)
        KeyCommand.RIGHT_CHAR -> moveCursor(Direction.Right)
        KeyCommand.RIGHT_WORD -> {}
        KeyCommand.LEFT_WORD -> {}
        KeyCommand.NEXT_PARAGRAPH -> {}
        KeyCommand.PREV_PARAGRAPH -> {}
        KeyCommand.LINE_START -> moveCursorAtStartOfLine()
        KeyCommand.LINE_END -> moveCursorAtEndOfLine()
        KeyCommand.LINE_LEFT -> {}
        KeyCommand.LINE_RIGHT -> {}
        KeyCommand.UP -> moveCursor(Direction.Up)
        KeyCommand.DOWN -> moveCursor(Direction.Down)
        KeyCommand.CENTER -> {}
        KeyCommand.PAGE_UP -> {}
        KeyCommand.PAGE_DOWN -> {}
        KeyCommand.HOME -> {}
        KeyCommand.END -> {}
        KeyCommand.PASTE -> {}
        KeyCommand.CUT -> {}
        KeyCommand.DELETE_PREV_CHAR -> delete()
        KeyCommand.DELETE_NEXT_CHAR -> content.edit { deleteForward() }
        KeyCommand.DELETE_PREV_WORD -> {}
        KeyCommand.DELETE_NEXT_WORD -> {}
        KeyCommand.DELETE_FROM_LINE_START -> {}
        KeyCommand.DELETE_TO_LINE_END -> {}
        KeyCommand.SELECT_ALL -> {}
        KeyCommand.SELECT_LEFT_CHAR -> moveCursor(Direction.Left, select = true)
        KeyCommand.SELECT_RIGHT_CHAR -> moveCursor(Direction.Right, select = true)
        KeyCommand.SELECT_UP -> moveCursor(Direction.Up, select = true)
        KeyCommand.SELECT_DOWN -> moveCursor(Direction.Down, select = true)
        KeyCommand.SELECT_PAGE_UP -> {}
        KeyCommand.SELECT_PAGE_DOWN -> {}
        KeyCommand.SELECT_HOME -> {}
        KeyCommand.SELECT_END -> {}
        KeyCommand.SELECT_LEFT_WORD -> {}
        KeyCommand.SELECT_RIGHT_WORD -> {}
        KeyCommand.SELECT_NEXT_PARAGRAPH -> {}
        KeyCommand.SELECT_PREV_PARAGRAPH -> {}
        KeyCommand.SELECT_LINE_START -> moveCursorAtStartOfLine(select = true)
        KeyCommand.SELECT_LINE_END -> moveCursorAtEndOfLine(select = true)
        KeyCommand.SELECT_LINE_LEFT -> {}
        KeyCommand.SELECT_LINE_RIGHT -> {}
        KeyCommand.DESELECT -> collapseSelection()
        KeyCommand.NEW_LINE -> insert(lineSeparator)
        KeyCommand.TAB -> insert("    ")
        KeyCommand.CHARACTER_PALETTE -> {}

        else -> {
            if (event.utf16CodePoint != 0) {
                val char = event.utf16CodePoint.toChar()
                if (char.isDefined() && !char.isISOControl()) insert(char.toString())
            }
        }
    }
}
