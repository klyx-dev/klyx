package com.klyx.core.undo

import com.klyx.core.Owner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.TimeMark

/** Undo and redo history for one scope, identified by [scopeId]. Safe to call from any dispatcher. */
class UndoManager(
    val scopeId: String,
    private val config: HistoryConfig = HistoryConfig(),
) : HistoryView {

    private class Entry(
        val id: Long,
        var operation: UndoableOperation,
        val mark: TimeMark,
    )

    private val mutex = Mutex()
    private val undoStack = ArrayDeque<Entry>()
    private val redoStack = ArrayDeque<Entry>()
    private var disposed = false
    private var nextEntryId = 0L
    private var savePointId: Long? = 0L

    private val _canUndo = MutableStateFlow(false)
    private val _canRedo = MutableStateFlow(false)
    private val _undoLabel = MutableStateFlow<String?>(null)
    private val _redoLabel = MutableStateFlow<String?>(null)
    private val _hasUnsavedChanges = MutableStateFlow(false)

    override val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    override val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    override val undoLabel: StateFlow<String?> = _undoLabel.asStateFlow()
    override val redoLabel: StateFlow<String?> = _redoLabel.asStateFlow()
    override val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()


    /** Applies [operation] and records it, clearing the redo stack. */
    suspend fun perform(operation: UndoableOperation) {
        withScope {
            executeAndRecord(operation)
        }
    }


    /**
     * Groups everything executed through [block] into one entry labelled [label]. A transaction that
     * executes nothing records nothing. If [block] throws or is cancelled, every operation it applied is
     * rolled back and nothing is recorded.
     */
    suspend fun <R> transaction(label: String, block: suspend TransactionScope.() -> R): R {
        return withScope {
            val applied = ArrayList<UndoableOperation>()
            val scope = ScopeImpl(applied)
            val result = try {
                scope.block()
            } catch (failure: Throwable) {
                withContext(NonCancellable) {
                    for (op in applied.asReversed()) {
                        runCatching { op.undo() }.onFailure {
                            if (it is CancellationException) {
                                ensureActive()
                            }
                        }
                    }
                }
                throw failure
            }
            when (applied.size) {
                0 -> {}
                1 -> record(applied[0])
                else -> record(CompositeOperation(label, applied.toList()))
            }
            result
        }
    }

    private inner class ScopeImpl(private val collected: MutableList<UndoableOperation>) : TransactionScope {
        override suspend fun execute(operation: UndoableOperation) {
            operation.execute()
            collected += operation
        }

        override suspend fun record(operation: UndoableOperation) {
            collected += operation
        }

        override suspend fun <R> transaction(label: String, block: suspend TransactionScope.() -> R): R {
            val nestedApplied = ArrayList<UndoableOperation>()
            val nested = ScopeImpl(nestedApplied)
            val result = try {
                nested.block()
            } catch (failure: Throwable) {
                withContext(NonCancellable) {
                    for (op in nestedApplied.asReversed()) {
                        runCatching { op.undo() }.onFailure {
                            if (it is CancellationException) {
                                ensureActive()
                            }
                        }
                    }
                }
                throw failure
            }
            when (nestedApplied.size) {
                0 -> {}
                1 -> collected += nestedApplied[0]
                else -> collected += CompositeOperation(label, nestedApplied.toList())
            }
            return result
        }
    }

    /** Reverts the newest entry. Returns false, doing nothing, when there is nothing to undo. */
    @IgnorableReturnValue
    suspend fun undo(): Boolean = withScope {
        val entry = undoStack.removeLastOrNull() ?: return@withScope false
        try {
            entry.operation.undo()
        } catch (failure: Throwable) {
            undoStack.addLast(entry)
            throw failure
        }
        redoStack.addLast(entry)
        publishState()
        true
    }

    /** Reapplies the newest undone entry. Returns false, doing nothing, when there is nothing to redo. */
    @IgnorableReturnValue
    suspend fun redo(): Boolean = withScope {
        val entry = redoStack.removeLastOrNull() ?: return@withScope false
        try {
            entry.operation.redo()
        } catch (failure: Throwable) {
            redoStack.addLast(entry)
            throw failure
        }
        undoStack.addLast(entry)
        publishState()
        true
    }


    /** Drops all history without touching current state. Neither undo nor redo is possible afterwards. */
    suspend fun clear() = withScope {
        undoStack.clear()
        redoStack.clear()
        savePointId = null
        publishState()
    }


    /** Marks the current position as saved, clearing [HistoryView.hasUnsavedChanges]. */
    suspend fun markSaved() = withScope {
        savePointId = undoStack.lastOrNull()?.id
        publishUnsaved()
    }


    /** Releases this history. Every later call throws [HistoryDisposedException]. */
    suspend fun dispose() = withScope {
        undoStack.clear()
        redoStack.clear()
        disposed = true
    }


    /**
     * Reverts and discards [owner]'s entries, and everything recorded above them, since undoing past
     * them would apply a change out of order. Entries below are left alone.
     */
    suspend fun invalidateOwner(owner: Owner): Unit = withScope {
        revertAndDropFromTopWhile(owner)
        dropFromTopWhile(redoStack, owner)
        publishState()
    }

    private suspend fun revertAndDropFromTopWhile(owner: Owner) {
        var cut = -1
        for (i in undoStack.indices.reversed()) {
            if (undoStack[i].operation.isOwnedBy(owner)) {
                cut = i; break
            }
        }
        if (cut < 0) return
        while (undoStack.size > cut) {
            val entry = undoStack.removeLast()
            entry.operation.undo()
        }
    }

    private fun dropFromTopWhile(stack: ArrayDeque<Entry>, owner: Owner) {
        var cut = -1
        for (i in stack.indices.reversed()) {
            if (stack[i].operation.isOwnedBy(owner)) {
                cut = i; break
            }
        }
        if (cut >= 0) repeat(stack.size - cut) { stack.removeLastOrNull() }
    }

    private suspend fun executeAndRecord(operation: UndoableOperation) {
        operation.execute()
        record(operation)
    }


    private fun record(operation: UndoableOperation) {
        redoStack.clear()
        val key = operation.coalescingKey
        val top = undoStack.lastOrNull()
        val merged =
            if (key != null && top?.operation?.coalescingKey == key && top.mark.elapsedNow() <= config.coalescingWindow) {
                top.operation.mergeWith(operation)
            } else null

        if (merged != null) {


            undoStack.removeLast()
            undoStack.addLast(Entry(nextEntryId++, merged, config.timeSource.markNow()))
        } else {
            undoStack.addLast(Entry(nextEntryId++, operation, config.timeSource.markNow()))
            while (undoStack.size > config.maxEntries) undoStack.removeFirstOrNull()
        }

        publishState()
    }

    private fun publishState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _undoLabel.value = undoStack.lastOrNull()?.operation?.label
        _redoLabel.value = redoStack.lastOrNull()?.operation?.label
        publishUnsaved()
    }

    private fun publishUnsaved() {
        val topId = undoStack.lastOrNull()?.id
        _hasUnsavedChanges.value = topId != savePointId
    }

    private suspend fun <R> withScope(block: suspend () -> R): R {
        val context = currentCoroutineContext()
        if (context[TransactionElement]?.manager === this) {
            if (disposed) throw HistoryDisposedException()
            return block()
        }
        return mutex.withLock {
            if (disposed) throw HistoryDisposedException()
            withContext(TransactionElement(this)) { block() }
        }
    }
}

private class TransactionElement(val manager: UndoManager) : CoroutineContext.Element {
    override val key get() = TransactionElement

    companion object Key : CoroutineContext.Key<TransactionElement>
}
