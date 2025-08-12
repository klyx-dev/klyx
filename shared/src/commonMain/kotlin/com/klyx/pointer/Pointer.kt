package com.klyx.pointer

import com.klyx.borrow.BorrowError
import com.klyx.borrow.Owned
import com.klyx.borrow.PointerRegistry
import com.klyx.borrow.deref
import com.klyx.borrow.derefMut
import kotlin.jvm.JvmInline

@JvmInline
value class Pointer(val raw: Long) {
    override fun toString() = "0x${raw.toString(16)}"

    fun isValid() = isValidPtr(this)

    fun <T : Any> borrow() = deref<T>(this)
    fun <T : Any> borrowMut() = derefMut<T>(this)

    fun delete() = dropPtr(this)

    companion object {
        val Invalid = Pointer(-1L)
    }
}

fun Long.asPointer() = Pointer(this)

fun <T : Any> Pointer.value() = borrow<T>().use { it.get() }
fun <T : Any> Pointer.valueMut() = borrowMut<T>().use { it.getMut() }


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

