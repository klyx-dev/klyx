package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory

@OptIn(ExperimentalWasmApi::class)
@JvmInline
value class WasmUByte(val value: UByte) : WasmValue {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmU8" }
        buffer[offset] = value.toByte()
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString() = value.toString()
    override fun toString(memory: WasmMemory) = toString()

    fun toUByte() = value

    companion object : HasWasmReader<WasmUByte> {
        const val SIZE_BYTES = 1
        val Zero = WasmUByte(0u)
        val Max = WasmUByte(UByte.MAX_VALUE)

        override val reader
            get() = object : WasmMemoryReader<WasmUByte> {
                override fun read(memory: WasmMemory, offset: Int) = WasmUByte(memory.uint8(offset))
                override val elementSize: Int = SIZE_BYTES
            }
    }
}

@OptIn(ExperimentalWasmApi::class)
@JvmInline
value class WasmByte(val value: Byte) : WasmValue {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmS8" }
        buffer[offset] = value
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString() = value.toString()
    override fun toString(memory: WasmMemory) = toString()

    fun toByte() = value

    companion object : HasWasmReader<WasmByte> {
        const val SIZE_BYTES = 1
        val Zero = WasmByte(0)
        val Min = WasmByte(Byte.MIN_VALUE)
        val Max = WasmByte(Byte.MAX_VALUE)

        override val reader
            get() = object : WasmMemoryReader<WasmByte> {
                override fun read(memory: WasmMemory, offset: Int) = WasmByte(memory.int8(offset))
                override val elementSize: Int = SIZE_BYTES
            }
    }
}
