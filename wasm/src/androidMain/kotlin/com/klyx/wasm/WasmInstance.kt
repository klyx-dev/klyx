package com.klyx.wasm

import com.dylibso.chicory.runtime.Instance

internal fun Instance.asWasmInstance() = WasmInstance(this)

class WasmInstance internal constructor(
    private val instance: Instance
) {
    val memory by lazy { WasmMemory(instance.memory()) }

    init {
        println("IMPORTS: ${instance.imports().functions().joinToString { it.name() }}")
        println("IMPORTS: ${instance.imports().globals().joinToString { it.name() }}")
        println("IMPORTS: ${instance.imports().memories().joinToString { it.name() }}")
        for (i in 0 until instance.module().exportSection().exportCount()) {
            println("EXPORTS: ${instance.module().exportSection().getExport(i).name()}")
        }
    }

    fun function(name: String) = instance.export(name).toWasmHostCallable()
}
