package com.klyx.extension.api

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory

// Use a generic sealed class (not interface) for Result
sealed class Result<out T, out E : Throwable> {
    data class Ok<T>(val value: T) : Result<T, Nothing>()
    data class Err<E : Throwable>(val error: E) : Result<Nothing, E>()

    val isOk: Boolean get() = this is Ok<T>
    val isErr: Boolean get() = this is Err<E>
}

@Suppress("UNCHECKED_CAST")
fun <T> Result(code: Int, err: String? = null): Result<T, Throwable> = when (code) {
    0 -> Result.Ok(null as T)
    1 -> Result.Err(Throwable(err ?: "Unknown error"))
    else -> throw IllegalArgumentException("Invalid result code: $code")
}

fun <T> Result(code: Byte, err: String? = null) = Result<T>(code.toInt(), err)
fun <T> Result(code: UByte, err: String? = null) = Result<T>(code.toInt(), err)

@OptIn(ExperimentalWasmApi::class)
fun WasmMemory.parseResult(addr: Int) = Result<Unit>(uint8(addr))
