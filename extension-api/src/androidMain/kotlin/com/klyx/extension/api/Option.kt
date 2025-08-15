package com.klyx.extension.api

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.WasmValue

sealed class Option<out T> {
    data object None : Option<Nothing>()
    data class Some<T>(val value: T) : Option<T>()

    inline val isSome: Boolean get() = this is Some<T>
    inline val isNone: Boolean get() = this is None
}

inline fun <T> Option<T>.onSome(block: (T) -> Unit): Option<T> {
    if (this is Option.Some) block(value)
    return this
}

inline fun <T> Option<T>.onNone(block: () -> Unit): Option<T> {
    if (this is Option.None) block()
    return this
}

/**
 * [tag]: 0 = None, 1 = Some
 */
fun <T> Option(tag: Int, value: T? = null): Option<T> = when (tag) {
    0 -> Option.None
    1 -> Option.Some(requireNotNull(value) { "Some(...) value cannot be null" })
    else -> throw IllegalArgumentException("Invalid Option tag: $tag")
}

fun <T> Option(tag: Byte, value: T? = null) = Option<T>(tag.toInt(), value)
fun <T> Option(tag: UByte, value: T? = null) = Option<T>(tag.toInt(), value)

fun <T> Some(value: T) = Option.Some(value)
typealias None = Option.None

@OptIn(ExperimentalWasmApi::class)
context(memory: WasmMemory)
fun <T> Option<T>.toWasmOption(): com.klyx.wasm.type.Option<WasmValue> {
    return when (this) {
        is Option.Some -> com.klyx.wasm.type.Some(WasmValue(value))
        is Option.None -> com.klyx.wasm.type.None
    }
}

fun <T> T?.asKlyxOption(): Option<T> = if (this == null) None else Some(this)

fun <T> Option<T>.asKotlinValue() = when (this) {
    is Option.None -> null
    is Option.Some -> value
}
