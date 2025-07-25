package com.klyx.wasm

import com.dylibso.chicory.runtime.Instance

internal fun Instance.asWasmInstance() = WasmInstance(this)

class WasmInstance internal constructor(
    private val instance: Instance
) {
    val memory by lazy { WasmMemory(instance.memory()) }
}
