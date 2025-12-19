package com.klyx.core.internal.borrow

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal class BorrowTracker {
    private val lock = SynchronizedObject()
    private val immutableBorrows = atomic(0)
    private val hasMutableBorrow = atomic(false)

    var state = BorrowState.Owned
        private set
        get() = synchronized(lock) { field }

    fun borrowImmutable() = synchronized(lock) {
        when (state) {
            BorrowState.Owned -> if (!hasMutableBorrow.value) {
                immutableBorrows.incrementAndGet()
                true
            } else false

            else -> false
        }
    }

    fun borrowMutable() = synchronized(lock) {
        when (state) {
            BorrowState.Owned -> if (immutableBorrows.value == 0 && !hasMutableBorrow.value) {
                hasMutableBorrow.value = true
                true
            } else false

            else -> false
        }
    }

    fun releaseImmutable() {
        synchronized(lock) {
            immutableBorrows.decrementAndGet()
        }
    }

    fun releaseMutable() {
        synchronized(lock) {
            immutableBorrows.decrementAndGet()
        }
    }

    fun move() = synchronized(lock) {
        when (state) {
            BorrowState.Owned -> {
                if (immutableBorrows.value == 0 && !hasMutableBorrow.value) {
                    state = BorrowState.Moved
                    true
                } else false
            }

            else -> false
        }
    }

    fun drop() = synchronized(lock) {
        when (state) {
            BorrowState.Owned -> {
                if (immutableBorrows.value == 0 && !hasMutableBorrow.value) {
                    state = BorrowState.Dropped
                    true
                } else false
            }

            else -> false
        }
    }

    fun isValid() = synchronized(lock) { state == BorrowState.Owned }
}
