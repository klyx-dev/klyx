package com.klyx.wasm

import com.dylibso.chicory.runtime.ImportFunction
import com.dylibso.chicory.runtime.Store

@ExperimentalWasmApi
class WasmStore internal constructor(
    internal val store: Store = Store()
) {
    fun instantiate(name: String, module: WasmModule): WasmInstance {
        return store.instantiate(name, module.module).also { instance ->
            instance.imports().functions().forEach {
                println("${it.name()}${it.paramTypes()} -> ${it.returnTypes()}")
            }

            for (i in 0 until instance.module().exportSection().exportCount()) {
                val export = instance.module().exportSection().getExport(i)
                val type = instance.type(i)
                println("${export.name()}${type.params()} -> ${type.returns().ifEmpty { "null" }}")
            }
        }.asWasmInstance()
    }
}

@OptIn(ExperimentalWasmApi::class)
@PublishedApi
internal fun WasmStore.addFunction(vararg function: ImportFunction) = apply { store.addFunction(*function) }
