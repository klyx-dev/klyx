package com.klyx.wasm

@ExperimentalWasmApi
interface WasmAny {
    fun writeToBuffer(buffer: ByteArray, offset: Int = 0)

    /**
     * Returns the size in bytes that this object will occupy in WASM memory.
     */
    fun sizeInBytes(): Int

    fun toString(memory: WasmMemory): String
}

@ExperimentalWasmApi
fun WasmAny.toBuffer(): ByteArray = ByteArray(sizeInBytes()).also { writeToBuffer(it) }
