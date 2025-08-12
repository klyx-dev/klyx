package com.klyx.core.borrow

// Immutable reference - multiple can exist simultaneously
class BorrowRef<T : Any> internal constructor(
    private val value: T,
    private val tracker: BorrowTracker
) : AutoCloseable {
    private var isReleased = false

    fun get(): T {
        if (isReleased) useAfterDropError("BorrowRef has been released")
        if (!tracker.isValid()) useAfterDropError("Referenced object has been dropped")
        return value
    }

    fun ptr() = PointerRegistry.getPointer(value)

    override fun close() {
        if (!isReleased) {
            tracker.releaseImmutable()
            isReleased = true
        }
    }

    override fun toString(): String {
        return if (isReleased) {
            "BorrowRef<RELEASED>"
        } else {
            "BorrowRef<${value::class.simpleName}>(ptr=0x${ptr().toString(16)})"
        }
    }
}

// Mutable reference - only one can exist at a time
class BorrowMutRef<T : Any> internal constructor(
    private val value: T,
    private val tracker: BorrowTracker
) : AutoCloseable {
    private var isReleased = false

    fun get(): T {
        if (isReleased) throw UseAfterDropError("BorrowMutRef has been released")
        if (!tracker.isValid()) throw UseAfterDropError("Referenced object has been dropped")
        return value
    }

    fun getMut(): T = get() // In Kotlin, mutability is more flexible

    fun ptr() = PointerRegistry.getPointer(value)

    override fun close() {
        if (!isReleased) {
            tracker.releaseMutable()
            isReleased = true
        }
    }

    override fun toString(): String {
        return if (isReleased) {
            "BorrowMutRef<RELEASED>"
        } else {
            "BorrowMutRef<${value::class.simpleName}>(ptr=0x${ptr().toString(16)})"
        }
    }
}
