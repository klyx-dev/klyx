package com.klyx.core.undo

import com.klyx.core.Owner
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** Marks a run of same key operations, such as consecutive keystrokes, that can merge into one undo step. */
@JvmInline
value class CoalescingKey(val name: String)

/**
 * One reversible change. [execute] and [undo] must be able to fail atomically: if [undo] throws, the state
 * is presumed unchanged. [redo] defaults to [execute] because most changes are simply reapplied.
 */
interface UndoableOperation {

    /** Human readable name, shown in menus as "Undo Format Document". */
    val label: String get() = "Edit"

    /** The owner whose unload invalidates this entry, or null to make it permanent. */
    val owner: Owner? get() = null

    /** Set on operations that should merge with an adjacent one carrying the same key. */
    val coalescingKey: CoalescingKey? get() = null

    suspend fun execute()
    suspend fun undo()
    suspend fun redo() = execute()

    /** Returns a merged operation covering both, or null to keep them as separate steps. */
    fun mergeWith(next: UndoableOperation): UndoableOperation? = null

    fun isOwnedBy(candidate: Owner): Boolean = owner == candidate
}

/** Several operations that undo and redo as one. Children run forward on redo and backward on undo. */
class CompositeOperation(
    override val label: String,
    val children: List<UndoableOperation>,
) : UndoableOperation {

    override suspend fun execute() = step(children, { it.execute() }, { it.undo() })
    override suspend fun undo() = step(children.asReversed(), { it.undo() }, { it.redo() })
    override suspend fun redo() = step(children, { it.redo() }, { it.undo() })

    override fun isOwnedBy(candidate: Owner): Boolean = children.any { it.isOwnedBy(candidate) }

    /** If a child fails, compensates the ones already applied before rethrowing. */
    private suspend fun step(
        order: List<UndoableOperation>,
        forward: suspend (UndoableOperation) -> Unit,
        compensate: suspend (UndoableOperation) -> Unit,
    ) {
        val done = ArrayList<UndoableOperation>(order.size)
        try {
            for (child in order) {
                forward(child)
                done += child
            }
        } catch (failure: Throwable) {
            withContext(NonCancellable) {
                for (child in done.asReversed()) {
                    try {
                        compensate(child)
                    } catch (secondary: Throwable) {
                        failure.addSuppressed(secondary)
                    }
                }
            }
            throw failure
        }
    }
}

/** How much history to keep and how long same key operations may merge. */
class HistoryConfig(
    val maxEntries: Int = 1_000,
    val coalescingWindow: Duration = 2.seconds,
    val timeSource: TimeSource = TimeSource.Monotonic,
) {
    init {
        require(maxEntries >= 1) { "maxEntries must be >= 1" }
    }
}

/** Read-only view of a history, for binding an Undo action's availability to. */
interface HistoryView {
    val canUndo: StateFlow<Boolean>
    val canRedo: StateFlow<Boolean>

    /** Label of the step that would be undone, for the menu item. */
    val undoLabel: StateFlow<String?>

    /** Label of the step that would be redone, for the menu item. */
    val redoLabel: StateFlow<String?>

    val hasUnsavedChanges: StateFlow<Boolean>
}

data class HistoryStep(val entryId: Long, val label: String)

enum class HistoryAction { None, Execute, Undo, Redo, Rollback }

/** Collects operations so they can be recorded as one undo entry. */
interface TransactionScope {

    /** Applies [operation] and records it. */
    suspend fun execute(operation: UndoableOperation)

    /** Records [operation] without applying it, for a step already performed elsewhere. */
    suspend fun record(operation: UndoableOperation)

    /** Groups nested operations into a single step of the enclosing transaction. */
    suspend fun <R> transaction(label: String, block: suspend TransactionScope.() -> R): R
}

class HistoryDisposedException : IllegalStateException("History has been disposed")
