package com.klyx.extension.internal

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.annotation.UnsafeResultErrorAccess
import com.github.michaelbull.result.annotation.UnsafeResultValueAccess
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.Err
import com.klyx.wasm.type.Ok
import com.klyx.wasm.type.WasmType

@OptIn(ExperimentalWasmApi::class, UnsafeResultValueAccess::class, UnsafeResultErrorAccess::class)
context(memory: WasmMemory)
fun <T, E> Result<T, E>.toWasmResult(): com.klyx.wasm.type.Result<WasmType, WasmType> {
    return when {
        isOk -> Ok(WasmType(value))
        isErr -> Err(WasmType(error))
        else -> error("Invalid Result state")
    }
}
