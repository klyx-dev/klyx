package com.klyx.editor.compose

import androidx.compose.runtime.Stable
import com.klyx.editor.compose.text.TextChange

@Stable
class UndoRedoManager internal constructor() {
    private val undoStack = ArrayDeque<List<TextChange>>()
    private val redoStack = ArrayDeque<List<TextChange>>()

    fun push(changes: List<TextChange>) {
        undoStack.addFirst(changes)
    }

    fun undo() = undoStack.removeFirst().also { redoStack.addFirst(it) }
    fun redo() = redoStack.removeFirst().also { undoStack.addFirst(it) }

    fun canUndo() = undoStack.isNotEmpty()
    fun canRedo() = redoStack.isNotEmpty()

    @Stable
    internal fun clearUndo() = undoStack.clear()

    @Stable
    internal fun clearRedo() = redoStack.clear()

    fun clear() {
        clearUndo()
        clearRedo()
    }

    override fun toString(): String {
        return "UndoRedoManager(undoStack=$undoStack, redoStack=$redoStack)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UndoRedoManager

        if (undoStack != other.undoStack) return false
        if (redoStack != other.redoStack) return false

        return true
    }

    override fun hashCode(): Int {
        var result = undoStack.hashCode()
        result = 31 * result + redoStack.hashCode()
        return result
    }
}
