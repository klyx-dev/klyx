package com.klyx.editor.compose.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.Direction
import com.klyx.editor.compose.moveCursorAtEndOfLine
import com.klyx.editor.compose.moveCursorAtStartOfLine
import com.klyx.lineSeparator

internal fun CodeEditorState.handleKeyEvent(event: KeyEvent) {
    if (event.type != KeyEventType.KeyDown) return

    when (event.key) {
        Key.Backspace -> delete()
        Key.DirectionRight -> moveCursor(Direction.Right, select = event.isShiftPressed)
        Key.DirectionLeft -> moveCursor(Direction.Left, select = event.isShiftPressed)
        Key.DirectionUp -> moveCursor(Direction.Up, select = event.isShiftPressed)
        Key.DirectionDown -> moveCursor(Direction.Down, select = event.isShiftPressed)
        Key.Enter -> insert(lineSeparator)
        Key.MoveHome -> moveCursorAtStartOfLine(select = event.isShiftPressed)
        Key.MoveEnd -> moveCursorAtEndOfLine(select = event.isShiftPressed)

        else -> {
            if (event.utf16CodePoint != 0) {
                val char = event.utf16CodePoint.toChar()
                if (char.isDefined() && !char.isISOControl()) insert(char.toString())
            }
        }
    }
}
