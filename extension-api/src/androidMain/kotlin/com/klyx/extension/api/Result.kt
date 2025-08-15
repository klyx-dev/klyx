package com.klyx.extension.api

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.WasmValue

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

fun <T> Ok(value: T) = Result.Ok(value)
fun <E> Err(error: E) = Result.Err(error)

@OptIn(ExperimentalWasmApi::class)
context(memory: WasmMemory)
fun <T, E> Result<T, E>.toWasmResult(): com.klyx.wasm.type.Result<WasmValue, WasmValue> {
    return when (this) {
        is Result.Ok -> com.klyx.wasm.type.Ok(WasmValue(value))
        is Result.Err -> com.klyx.wasm.type.Err(WasmValue(error))
    }
}

fun <T, E> Result<T, E>.asKotlinResult() = when (this) {
    is Result.Err -> kotlin.Result.failure(Exception("$error"))
    is Result.Ok -> kotlin.Result.success(value)
}

fun <T> kotlin.Result<T>.asKlyxResult() = fold(::Ok, ::Err)
