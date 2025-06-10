package com.klyx.editor.input

import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import com.klyx.core.event.EventBus
import com.klyx.core.event.toComposeKeyEvent
import com.klyx.editor.CodeEditorState
import com.klyx.editor.cursor.CursorPosition

class EditorInputConnection(
    view: View,
    private val editorState: CodeEditorState,
) : BaseInputConnection(view, false) {
    override fun sendKeyEvent(e: KeyEvent): Boolean {
        val event = e.toComposeKeyEvent()
        EventBus.getInstance().postSync(event)

        if (event.type == KeyEventType.KeyDown) {
            when (event.key) {
                Key.DirectionRight -> editorState.moveCursor(CursorPosition.Direction.Right)
                Key.DirectionLeft -> editorState.moveCursor(CursorPosition.Direction.Left)
                Key.DirectionUp -> editorState.moveCursor(CursorPosition.Direction.Up)
                Key.DirectionDown -> editorState.moveCursor(CursorPosition.Direction.Down)
                Key.MoveEnd -> editorState.moveCursorAtEndOfLine()
                Key.MoveHome -> editorState.moveCursorAtStartOfLine()
                Key.Backspace -> editorState.deleteAtCursor(1)
                Key.Enter -> editorState.insertAtCursor("\n")

                else -> {
                    if (event.utf16CodePoint != 0) {
                        val char = event.utf16CodePoint.toChar()

                        if (char.isDefined() && !char.isISOControl()) {
                            editorState.insertAtCursor(char.toString())
                        }
                    }
                }
            }
        }

        return true
    }
}
