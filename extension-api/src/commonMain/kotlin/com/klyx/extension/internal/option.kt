package com.klyx.extension.internal

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.WasmType

@OptIn(ExperimentalWasmApi::class)
context(memory: WasmMemory)
fun <T> Option<T>.toWasmOption(): com.klyx.wasm.type.Option<WasmType> {
    return when (this) {
        None -> com.klyx.wasm.type.None
        is Some -> com.klyx.wasm.type.Some(WasmType(value))
    }
}
