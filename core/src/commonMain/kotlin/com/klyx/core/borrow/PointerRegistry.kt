package com.klyx.core.borrow

import com.klyx.core.pointer.Pointer
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal object PointerRegistry {
    private val lock = SynchronizedObject()

    private val ptrCounter = atomic(0x1000L)
    private val ptrToTracker = mutableMapOf<Long, BorrowTracker>()
    private val ptrToOwned = mutableMapOf<Long, Owned<*>>()
    private val ptrToValue = mutableMapOf<Long, Any>()

    fun allocatePointer(value: Any, tracker: BorrowTracker, owned: Owned<*>) = synchronized(lock) {
        val ptr = ptrCounter.getAndIncrement()
        ptrToTracker[ptr] = tracker
        ptrToOwned[ptr] = owned
        ptrToValue[ptr] = value
        Pointer(ptr)
    }

    fun deallocatePointer(ptr: Pointer) {
        synchronized(lock) {
            val raw = ptr.raw
            ptrToTracker.remove(raw)
            ptrToOwned.remove(raw)
            ptrToValue.remove(raw)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValue(pointer: Pointer) = synchronized(lock) {
        val ptr = pointer.raw

        val tracker = ptrToTracker[ptr]
            ?: invalidPointerError("Invalid pointer: 0x${ptr.toString(16)}")

        if (!tracker.isValid()) useAfterDropError("Use of dropped pointer: 0x${ptr.toString(16)}")
        val value = ptrToValue[ptr]
        value as? T ?: throw ClassCastException("Value at pointer is not of expected type")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOwned(pointer: Pointer) = synchronized(lock) {
        val ptr = pointer.raw

        val owned = ptrToOwned[ptr]
            ?: invalidPointerError("No owned found for pointer: 0x${ptr.toString(16)}")
        val tracker = ptrToTracker[ptr]
            ?: invalidPointerError("Invalid pointer: 0x${ptr.toString(16)}")

        if (!tracker.isValid()) useAfterDropError("Use of dropped pointer: 0x${ptr.toString(16)}")
        owned as? Owned<T> ?: throw ClassCastException("Owned at pointer is not of expected type")
    }

    fun getPointer(value: Any?) = synchronized(lock) {
        ptrToValue.entries.find { it.value === value }?.key
            ?: useAfterDropError("No pointer found for value: $value")
    }

    fun getTracker(pointer: Pointer) = synchronized(lock) {
        ptrToTracker[pointer.raw]
    }

    fun isValidPointer(pointer: Pointer) = synchronized(lock) {
        val tracker = ptrToTracker[pointer.raw] ?: return@synchronized false
        tracker.isValid()
    }

    // dereference pointer - create a new borrow from pointer
    fun <T : Any> deref(pointer: Pointer): BorrowRef<T> {
        val tracker = getTracker(pointer)
            ?: invalidPointerError("Invalid pointer: $pointer")
        val value = getValue<T>(pointer)

        if (!tracker.borrowImmutable()) {
            when (tracker.state) {
                BorrowState.Moved -> borrowWhileMovedError("Cannot dereference: object has been moved")
                BorrowState.Dropped -> useAfterDropError("Cannot dereference: object has been dropped")
                else -> doubleBorrowError("Cannot dereference: mutable borrow exists")
            }
        }

        return BorrowRef(value, tracker)
    }

    // dereference pointer mutably
    fun <T : Any> derefMut(ptr: Pointer): BorrowMutRef<T> {
        val tracker = getTracker(ptr)
            ?: invalidPointerError("Invalid pointer: $ptr")
        val value = getValue<T>(ptr)

        if (!tracker.borrowMutable()) {
            when (tracker.state) {
                BorrowState.Moved -> borrowWhileMovedError("Cannot dereference mutably: object has been moved")
                BorrowState.Dropped -> useAfterDropError("Cannot dereference mutably: object has been dropped")
                else -> doubleBorrowError("Cannot dereference mutably: other borrows exist")
            }
        }

        return BorrowMutRef(value, tracker)
    }

    // internal use only
    fun clear() {
        synchronized(lock) {
            ptrToTracker.clear()
            ptrToOwned.clear()
            ptrToValue.clear()
        }
    }
}

@JvmName("refValue")
fun <T : Any> ref(value: T) = value.owned()
fun <T : Any> T.ref() = this.owned()

fun <T : Any> deref(ptr: Pointer): BorrowRef<T> = PointerRegistry.deref(ptr)
fun <T : Any> derefMut(ptr: Pointer): BorrowMutRef<T> = PointerRegistry.derefMut(ptr)

fun isValidPtr(ptr: Pointer): Boolean = PointerRegistry.isValidPointer(ptr)

fun dropPtr(ptr: Pointer): Boolean {
    return try {
        val owned = PointerRegistry.getOwned<Any>(ptr)
        owned.drop()
        true
    } catch (e: BorrowError) {
        false
    }
}

fun <T : Any> movePtr(ptr: Pointer): Owned<T> {
    val owned = PointerRegistry.getOwned<T>(ptr)
    return owned.move()
}

