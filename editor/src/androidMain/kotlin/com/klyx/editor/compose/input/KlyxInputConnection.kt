package com.klyx.editor.compose.input

import android.util.Log
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputConnection
import androidx.compose.ui.input.key.KeyEvent
import com.klyx.core.event.asComposeKeyEvent
import com.klyx.editor.EditorState

class KlyxInputConnection(
    private val view: View,
    private val editorState: EditorState,
    private val onKeyEvent: (KeyEvent) -> Boolean,
    private val onCursorMoved: () -> Unit
) : BaseInputConnection(view, false) {

    override fun sendKeyEvent(event: android.view.KeyEvent): Boolean {
        return onKeyEvent(event.asComposeKeyEvent())
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
        
        // Normalize line endings in the input text
        val normalizedInput = text.toString().replace("\r\n", "\n").replace("\r", "\n")
        
        // Get text before and after cursor
        val beforeCursor = currentText.substring(0, cursorPosition)
        val afterCursor = currentText.substring(cursorPosition)
        
        // Create new text with normalized line endings
        val newText = beforeCursor + normalizedInput + afterCursor
        
        // Update editor state
        editorState.updateText(newText)
        
        // Handle cursor position based on whether this is a newline insertion
        if (normalizedInput == "\n") {
            // For newline, move cursor exactly one position forward
            editorState.moveCursor(cursorPosition + 1)
        } else {
            // For other text, move cursor after the inserted text
            editorState.moveCursor(cursorPosition + normalizedInput.length)
        }
        
        onCursorMoved()
        return true
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        val currentText = editorState.text
        val cursorPosition = editorState.cursorPosition
        
        if (beforeLength > 0 && cursorPosition > 0) {
            // Normalize line endings to \n for consistent handling
            val normalizedText = currentText.replace("\r\n", "\n").replace("\r", "\n")
            
            // Calculate the actual start position
            val start = (cursorPosition - beforeLength).coerceAtLeast(0)
            
            // Get the text before and after cursor
            val beforeCursor = normalizedText.substring(0, start)
            val afterCursor = normalizedText.substring(cursorPosition)
            
            // Create new text
            val newText = beforeCursor + afterCursor
            
            // Update editor state
            editorState.updateText(newText)
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
