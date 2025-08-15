package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmAny
import com.klyx.wasm.WasmMemory

/**
 * Interface for reading [WasmAny] instances from [WasmMemory].
 */
@ExperimentalWasmApi
interface WasmMemoryReader<T : WasmAny> {
    fun read(memory: WasmMemory, offset: Int): T

    fun read(memory: WasmMemory, ptr: Int, len: Int): T {
        return read(memory, ptr)
    }

    val elementSize: Int
}

@ExperimentalWasmApi
interface HasWasmReader<T : WasmAny> {
    val reader: WasmMemoryReader<T>
}
