package com.klyx.wasm

import com.dylibso.chicory.wasm.WasmModule as Module

@ExperimentalWasmApi
class WasmModule internal constructor(
    internal val module: Module
) {
}

@OptIn(ExperimentalWasmApi::class)
internal fun Module.asWasmModule() = WasmModule(this)
