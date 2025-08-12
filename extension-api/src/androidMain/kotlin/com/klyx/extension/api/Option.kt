package com.klyx.extension.api

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
@Suppress("UNCHECKED_CAST")
fun <T> Option(tag: Int, value: T? = null): Option<T> = when (tag) {
    0 -> Option.None
    1 -> Option.Some(value as T)
    else -> throw IllegalArgumentException("Invalid Option tag: $tag")
}

fun <T> Option(tag: Byte, value: T? = null) = Option<T>(tag.toInt(), value)
fun <T> Option(tag: UByte, value: T? = null) = Option<T>(tag.toInt(), value)
