package com.klyx.core.borrow

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class Owned<T : Any>(private var value: T?) {
    private val tracker = BorrowTracker()

    private val pointer = if (value != null) {
        PointerRegistry.allocatePointer(value!!, tracker, this)
    } else {
        -1L
    }

    fun borrow(): BorrowRef<T> {
        if (!tracker.borrowImmutable()) {
            when (tracker.state) {
                BorrowState.Moved -> borrowWhileMovedError("Cannot borrow: object has been moved")
                BorrowState.Dropped -> useAfterDropError("Cannot borrow: object has been dropped")
                else -> doubleBorrowError("Cannot create immutable borrow: mutable borrow exists")
            }
        }

        return BorrowRef(value!!, tracker)
    }

    // Mutable borrow - only one allowed at a time
    fun borrowMut(): BorrowMutRef<T> {
        if (!tracker.borrowMutable()) {
            when (tracker.state) {
                BorrowState.Moved -> borrowWhileMovedError("Cannot borrow mutably: object has been moved")
                BorrowState.Dropped -> useAfterDropError("Cannot borrow mutably: object has been dropped")
                else -> doubleBorrowError("Cannot create mutable borrow: other borrows exist")
            }
        }
        return BorrowMutRef(value!!, tracker)
    }

    // Move ownership to another Owned instance
    fun move(): Owned<T> {
        if (!tracker.move()) {
            when (tracker.state) {
                BorrowState.Moved -> throw BorrowWhileMovedError("Cannot move: object already moved")
                BorrowState.Dropped -> throw UseAfterDropError("Cannot move: object has been dropped")
                else -> throw DoubleBorrowError("Cannot move: active borrows exist")
            }
        }

        val movedValue = value!!
        value = null
        return Owned(movedValue)
    }

    // Drop the owned value - makes it unusable
    fun drop() {
        if (!tracker.drop()) {
            when (tracker.state) {
                BorrowState.Moved -> {
                    // Clean up pointer registry even if already moved
                    if (pointer != -1L) PointerRegistry.deallocatePointer(pointer)
                    return
                }

                BorrowState.Dropped -> return // Already dropped
                else -> doubleBorrowError("Cannot drop: active borrows exist")
            }
        }
        value = null
        if (pointer != -1L) PointerRegistry.deallocatePointer(pointer)
    }

    // Get the value directly (consuming ownership)
    fun into(): T {
        val moved = move()
        val result = moved.value!!
        moved.drop()
        return result
    }

    // Check if the owned value is still valid
    fun isValid(): Boolean = tracker.isValid() && value != null

    fun ptr() = pointer

    fun deref(): T = if (isValid()) {
        PointerRegistry.getValue(pointer)
    } else {
        useAfterDropError("object is already dropped")
    }

    override fun toString(): String {
        return when {
            !isValid() -> "Owned<INVALID>"
            else -> {
                val name = if (value != null) value!!::class.simpleName else "null"
                "Owned<$name>(ptr=0x${ptr().toString(16)})"
            }
        }
    }
}

fun <T : Any> T.owned(): Owned<T> = Owned(this)
fun <T : Any> Owned<T>.valueNow() = borrow().use { it.get() }
fun <T : Any> Owned<T>.valueNowMut() = borrowMut().use { it.getMut() }

// usage with automatic cleanup
@OptIn(ExperimentalContracts::class)
inline fun <T : Any, R> Owned<T>.withBorrow(block: (BorrowRef<T>) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return borrow().use(block)
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Any, R> Owned<T>.withBorrowMut(block: (BorrowMutRef<T>) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return borrowMut().use(block)
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Any, R> Owned<T>.useValue(block: (T) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return borrow().use { block(it.get()) }
}

// get mutable value directly (auto borrow/close)
@OptIn(ExperimentalContracts::class)
inline fun <T : Any, R> Owned<T>.useValueMut(block: (T) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return borrowMut().use { block(it.getMut()) }
}
