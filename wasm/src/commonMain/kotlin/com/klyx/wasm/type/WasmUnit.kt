package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory

@OptIn(ExperimentalWasmApi::class)
object WasmUnit : WasmType {
    private val value = WasmInt(0)

    override fun createReader() = value.createReader()
    override fun writeToBuffer(buffer: ByteArray, offset: Int) = value.writeToBuffer(buffer, offset)
    override fun sizeInBytes(): Int = value.sizeInBytes()
    override fun toString(memory: WasmMemory) = value.toString(memory)
    override fun toString() = value.toString()
}
