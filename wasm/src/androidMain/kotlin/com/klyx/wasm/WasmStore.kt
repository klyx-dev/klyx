package com.klyx.wasm

import com.dylibso.chicory.runtime.ImportFunction
import com.dylibso.chicory.runtime.Store

@ExperimentalWasm
class WasmStore internal constructor(
    internal val store: Store = Store()
) {
    fun instantiate(name: String, module: WasmModule): WasmInstance {
        return store.instantiate(name, module.module).asWasmInstance()
    }
}

@OptIn(ExperimentalWasm::class)
@PublishedApi
internal fun WasmStore.addFunction(vararg function: ImportFunction) = apply { store.addFunction(*function) }
