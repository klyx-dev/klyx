package com.klyx.editor.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A state holder for the KlyxCodeEditor's content.
 * This class holds the text being edited and manages cursor position and selection.
 */
class EditorState(initialText: String = "") {
    var text by mutableStateOf(initialText)
        private set
    var isModified by mutableStateOf(false)

    // Cursor position (caret position)
    var cursorPosition by mutableIntStateOf(initialText.length)
        private set
    
    // Selection range (start and end positions)
    var selectionStart by mutableIntStateOf(initialText.length)
        private set
    var selectionEnd by mutableIntStateOf(initialText.length)
        private set
    
    // Whether there is an active selection
    val hasSelection: Boolean
        get() = selectionStart != selectionEnd
    
    /**
     * Moves the cursor to the specified position.
     * @param position The new cursor position
     * @param extendSelection If true, extends the selection from the current cursor position to the new position
     */
    fun moveCursor(position: Int, extendSelection: Boolean = false) {
        val newPosition = position.coerceIn(0, text.length)
        if (extendSelection) {
            selectionEnd = newPosition
        } else {
            cursorPosition = newPosition
            selectionStart = newPosition
            selectionEnd = newPosition
        }
    }
    
    /**
     * Sets the selection range.
     * @param start The start position of the selection
     * @param end The end position of the selection
     */
    fun setSelection(start: Int, end: Int) {
        val validStart = start.coerceIn(0, text.length)
        val validEnd = end.coerceIn(0, text.length)
        selectionStart = validStart
        selectionEnd = validEnd
        cursorPosition = validEnd
    }
    
    /**
     * Clears the current selection.
     */
    fun clearSelection() {
        selectionStart = cursorPosition
        selectionEnd = cursorPosition
    }
    
    /**
     * Gets the selected text.
     * @return The selected text, or empty string if no selection
     */
    fun getSelectedText(): String {
        return if (hasSelection) {
            val start = minOf(selectionStart, selectionEnd)
            val end = maxOf(selectionStart, selectionEnd)
            text.substring(start, end)
        } else {
            ""
        }
    }

    fun updateText(newText: String) {
        // normalize line endings to \n
        text = newText.replace("\r\n", "\n").replace("\r", "\n")
        isModified = true
    }

    fun markAsSaved() {
        isModified = false
    }
}
