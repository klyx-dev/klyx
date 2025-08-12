package com.klyx.wasm

@ExperimentalWasmApi
interface WasmAny {
    fun writeToMemory(memory: WasmMemory)
}
