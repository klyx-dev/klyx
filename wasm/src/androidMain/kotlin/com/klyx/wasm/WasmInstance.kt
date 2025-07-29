package com.klyx.wasm

import com.dylibso.chicory.runtime.Instance

@OptIn(ExperimentalWasm::class)
internal fun Instance.asWasmInstance() = WasmInstance(this)

@ExperimentalWasm
class WasmInstance internal constructor(
    private val instance: Instance
) {
    val memory by lazy { WasmMemory(instance.memory()) }

    fun function(name: String) = instance.export(name).toWasmHostCallable()
}
