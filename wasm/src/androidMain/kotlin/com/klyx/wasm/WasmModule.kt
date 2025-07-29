package com.klyx.wasm

import com.dylibso.chicory.wasm.WasmModule as Module

@ExperimentalWasm
class WasmModule internal constructor(
    internal val module: Module
) {
}

@OptIn(ExperimentalWasm::class)
internal fun Module.asWasmModule() = WasmModule(this)
