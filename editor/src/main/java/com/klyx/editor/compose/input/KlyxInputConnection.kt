package com.klyx.editor.compose.input

import android.util.Log
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputConnection
import androidx.compose.ui.input.key.KeyEvent
import com.klyx.editor.compose.EditorState

class KlyxInputConnection(
    private val view: View,
    private val editorState: EditorState,
    private val onKeyEvent: (KeyEvent) -> Boolean,
    private val onCursorMoved: () -> Unit
) : BaseInputConnection(view, false) {

    override fun sendKeyEvent(event: android.view.KeyEvent): Boolean {
        return onKeyEvent(KeyEvent(event))
    }

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (text == null) return false
        
        val currentText = editorState.text
        val cursorPosition = editorState.cursorPosition
        
        // Validate cursor position
        if (cursorPosition < 0 || cursorPosition > currentText.length) {
            Log.e("KlyxInputConnection", "Invalid cursor position: $cursorPosition, text length: ${currentText.length}")
            return false
        }
        
        // ensure we're using the current cursor position
        val beforeCursor = currentText.substring(0, cursorPosition)
        val afterCursor = currentText.substring(cursorPosition)
        
        // validate text parts
        if (beforeCursor.length + afterCursor.length != currentText.length) {
            Log.e("KlyxInputConnection", "Text parts validation failed: before=${beforeCursor.length}, after=${afterCursor.length}, total=${currentText.length}")
            return false
        }
        
        val newText = beforeCursor + text + afterCursor
        editorState.text = newText
        
        // validate new cursor position
        val newCursorPos = cursorPosition + text.length
        if (newCursorPos > newText.length) {
            Log.e("KlyxInputConnection", "Invalid new cursor position: $newCursorPos, new text length: ${newText.length}")
            return false
        }
        
        editorState.moveCursor(newCursorPos)
        onCursorMoved()
        return true
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        val currentText = editorState.text
        val cursorPosition = editorState.cursorPosition
        
        if (beforeLength > 0 && cursorPosition > 0) {
            val start = (cursorPosition - beforeLength).coerceAtLeast(0)
            val beforeCursor = currentText.substring(0, start)
            val afterCursor = currentText.substring(cursorPosition)
            
            // Validate text parts
            if (beforeCursor.length + afterCursor.length + beforeLength != currentText.length) {
                Log.e("KlyxInputConnection", "Delete validation failed: before=${beforeCursor.length}, after=${afterCursor.length}, delete=$beforeLength, total=${currentText.length}")
                return false
            }
            
            editorState.text = beforeCursor + afterCursor
            editorState.moveCursor(start)
            onCursorMoved()
            return true
        }
        return false
    }

    override fun getTextBeforeCursor(length: Int, flags: Int): CharSequence {
        val cursorPosition = editorState.cursorPosition
        val start = (cursorPosition - length).coerceAtLeast(0)
        return editorState.text.substring(start, cursorPosition)
    }

    override fun getTextAfterCursor(length: Int, flags: Int): CharSequence {
        val cursorPosition = editorState.cursorPosition
        val end = (cursorPosition + length).coerceAtMost(editorState.text.length)
        return editorState.text.substring(cursorPosition, end)
    }

    override fun getSelectedText(flags: Int): CharSequence {
        return editorState.getSelectedText()
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        editorState.setSelection(start, end)
        onCursorMoved()
        return true
    }

    companion object {
        fun create(
            view: View,
            editorState: EditorState,
            onKeyEvent: (KeyEvent) -> Boolean,
            onCursorMoved: () -> Unit
        ): InputConnection {
            return KlyxInputConnection(view, editorState, onKeyEvent, onCursorMoved)
        }
    }
}
