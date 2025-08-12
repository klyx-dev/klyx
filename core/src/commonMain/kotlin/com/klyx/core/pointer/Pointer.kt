package com.klyx.core.pointer

import com.klyx.core.borrow.deref
import com.klyx.core.borrow.derefMut
import com.klyx.core.borrow.dropPtr
import com.klyx.core.borrow.isValidPtr

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

