package com.klyx.wasm

import com.dylibso.chicory.wasm.WasmModule as Module

class WasmModule internal constructor(
    internal val module: Module
) {
}

internal fun Module.asWasmModule() = WasmModule(this)
