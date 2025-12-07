package com.klyx.extension.internal

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.Err
import com.klyx.wasm.type.Ok
import com.klyx.wasm.type.WasmString
import com.klyx.wasm.type.WasmType
import com.klyx.wasm.type.wstr
import io.itsvks.anyhow.AnyhowResult
import io.itsvks.anyhow.Result
import io.itsvks.anyhow.UnsafeResultApi
import kotlin.jvm.JvmName

@OptIn(ExperimentalWasmApi::class, UnsafeResultApi::class)
context(memory: WasmMemory)
fun <T, E> Result<T, E>.toWasmResult(): com.klyx.wasm.type.Result<WasmType, WasmType> {
    return when {
        isOk -> Ok(WasmType(value))
        isErr -> Err(WasmType(error))
        else -> error("Invalid Result state")
    }
}

@JvmName("_toWasmResult")
@OptIn(ExperimentalWasmApi::class, UnsafeResultApi::class)
context(memory: WasmMemory)
fun <T> AnyhowResult<T>.toWasmResult(): com.klyx.wasm.type.Result<WasmType, WasmString> {
    return when {
        isOk -> Ok(WasmType(value))
        isErr -> Err(error.toString().wstr)
        else -> error("Invalid Result state")
    }
}
