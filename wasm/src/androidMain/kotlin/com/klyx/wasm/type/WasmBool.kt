package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory

@OptIn(ExperimentalWasmApi::class)
@JvmInline
value class WasmBool(val value: Boolean) : WasmValue {

    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmBool" }
        buffer[offset] = if (value) 1 else 0
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString() = value.toString()
    override fun toString(memory: WasmMemory) = toString()

    fun toBoolean() = value

    companion object : HasWasmReader<WasmBool> {
        const val SIZE_BYTES = 1
        val True = WasmBool(true)
        val False = WasmBool(false)

        override val reader
            get() = object : WasmMemoryReader<WasmBool> {
                override fun read(memory: WasmMemory, offset: Int): WasmBool {
                    val byteValue = memory.uint8(offset)
                    return WasmBool(byteValue != 0u.toUByte())
                }

                override val elementSize: Int = SIZE_BYTES
            }
    }
}
