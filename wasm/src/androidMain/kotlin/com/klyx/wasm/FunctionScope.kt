package com.klyx.wasm

@ExperimentalWasmApi
class FunctionScope internal constructor(
    val instance: WasmInstance
) {
    val memory get() = instance.memory

    /**
     * Reads a UTF-8 string from WASM memory
     */
    val LongArray.string get() = instance.memory.readString(this)
    fun LongArray.string(offset: Int = 0) = instance.memory.readString(this, offset)
}
