package com.klyx.extension.api

sealed class Result<out T, out E> {
    data class Ok<T>(val value: T) : Result<T, Nothing>()
    data class Err<E>(val error: E) : Result<Nothing, E>()

    inline val isOk get() = this is Ok
    inline val isErr get() = this is Err

    inline fun onOk(block: (T) -> Unit): Result<T, E> {
        if (this is Ok) block(value)
        return this
    }

    inline fun onErr(block: (E) -> Unit): Result<T, E> {
        if (this is Err) block(error)
        return this
    }

    inline fun <R> map(transform: (T) -> R) = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> this
    }

    inline fun <F> mapErr(transform: (E) -> F) = when (this) {
        is Ok -> this
        is Err -> Err(transform(error))
    }
}

/**
 * [tag]: 0 = Ok, 1 = Err
 */
fun <T> Result(
    tag: Int,
    okValue: T?,
    err: String? = null
): Result<T, Throwable> = when (tag) {
    0 -> Result.Ok(requireNotNull(okValue) { "Ok value cannot be null" })
    1 -> Result.Err(Throwable(err ?: "Unknown error"))
    else -> throw IllegalArgumentException("Invalid result code: $tag")
}

fun <T> Result(tag: Byte, okValue: T?, err: String? = null) = Result(tag.toInt(), okValue, err)
fun <T> Result(tag: UByte, okValue: T?, err: String? = null) = Result(tag.toInt(), okValue, err)
