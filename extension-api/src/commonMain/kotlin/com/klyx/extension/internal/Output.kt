package com.klyx.extension.internal

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.Some
import com.klyx.wasm.type.asWasmU8Array
import com.klyx.wasm.type.wasm

data class Output(
    val status: Int,
    val stdout: String,
    val stderr: String
)

@OptIn(ExperimentalWasmApi::class)
context(memory: WasmMemory)
fun Output.toWasmOutput() = com.klyx.core.extension.internal.wasm.Output(
    status = Some(status.wasm),
    stdout = stdout.asWasmU8Array(),
    stderr = stderr.asWasmU8Array()
)
