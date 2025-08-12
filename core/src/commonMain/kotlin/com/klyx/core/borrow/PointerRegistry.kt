package com.klyx.core.borrow

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
        ptr
    }

    fun deallocatePointer(ptr: Long) {
        synchronized(lock) {
            ptrToTracker.remove(ptr)
            ptrToOwned.remove(ptr)
            ptrToValue.remove(ptr)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValue(ptr: Long) = synchronized(lock) {
        val tracker = ptrToTracker[ptr]
            ?: invalidPointerError("Invalid pointer: 0x${ptr.toString(16)}")

        if (!tracker.isValid()) useAfterDropError("Use of dropped pointer: 0x${ptr.toString(16)}")
        val value = ptrToValue[ptr]
        value as? T ?: throw ClassCastException("Value at pointer is not of expected type")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOwned(ptr: Long) = synchronized(lock) {
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

    fun getTracker(ptr: Long) = synchronized(lock) {
        ptrToTracker[ptr]
    }

    fun isValidPointer(ptr: Long) = synchronized(lock) {
        val tracker = ptrToTracker[ptr] ?: return@synchronized false
        tracker.isValid()
    }

    // dereference pointer - create a new borrow from pointer
    fun <T : Any> deref(ptr: Long): BorrowRef<T> {
        val tracker = getTracker(ptr)
            ?: invalidPointerError("Invalid pointer: 0x${ptr.toString(16)}")
        val value = getValue<T>(ptr)

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
    fun <T : Any> derefMut(ptr: Long): BorrowMutRef<T> {
        val tracker = getTracker(ptr)
            ?: invalidPointerError("Invalid pointer: 0x${ptr.toString(16)}")
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

fun <T : Any> deref(ptr: Long): BorrowRef<T> = PointerRegistry.deref(ptr)
fun <T : Any> derefMut(ptr: Long): BorrowMutRef<T> = PointerRegistry.derefMut(ptr)
fun <T : Any> ptrValue(ptr: Long): T = PointerRegistry.getValue(ptr)
fun <T : Any> ptrOwned(ptr: Long): Owned<T> = PointerRegistry.getOwned(ptr)
fun isValidPtr(ptr: Long): Boolean = PointerRegistry.isValidPointer(ptr)

fun dropPtr(ptr: Long): Boolean {
    return try {
        val owned = PointerRegistry.getOwned<Any>(ptr)
        owned.drop()
        true
    } catch (e: BorrowError) {
        false
    }
}

fun <T : Any> movePtr(ptr: Long): Owned<T> {
    val owned = PointerRegistry.getOwned<T>(ptr)
    return owned.move()
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Any, R> withDeref(ptr: Long, block: (BorrowRef<T>) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return deref<T>(ptr).use(block)
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Any, R> withDerefMut(ptr: Long, block: (BorrowMutRef<T>) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return derefMut<T>(ptr).use(block)
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Any, R> ptrUseValue(ptr: Long, block: (T) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return deref<T>(ptr).use { block(it.get()) }
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Any, R> ptrUseValueMut(ptr: Long, block: (T) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return derefMut<T>(ptr).use { block(it.getMut()) }
}

fun <T : Any> ptrValueNow(ptr: Long) = deref<T>(ptr).use { it.get() }
fun <T : Any> ptrValueMutNow(ptr: Long) = derefMut<T>(ptr).use { it.getMut() }

