package com.klyx.nodegraph

internal class UndoStack(private val limit: Int = 100) {
    private val undoStack = ArrayDeque<GraphCommand>(limit)
    private val redoStack = ArrayDeque<GraphCommand>(limit)

    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    fun push(cmd: GraphCommand) {
        if (undoStack.size >= limit) undoStack.removeFirst()
        undoStack.addLast(cmd)
        redoStack.clear()
    }

    fun undo(state: GraphState) {
        val cmd = undoStack.removeLastOrNull() ?: return
        cmd.undo(state)
        redoStack.addLast(cmd)
    }

    fun redo(state: GraphState) {
        val cmd = redoStack.removeLastOrNull() ?: return
        cmd.redo(state)
        undoStack.addLast(cmd)
    }

    fun clear() {
        undoStack.clear(); redoStack.clear()
    }
}
