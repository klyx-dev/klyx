package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.utils.writeInt16LE

@OptIn(ExperimentalWasmApi::class)
@JvmInline
value class WasmUShort(val value: UShort) : WasmValue {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmU16" }
        buffer.writeInt16LE(value.toShort(), offset)
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString() = value.toString()
    override fun toString(memory: WasmMemory) = toString()

    fun toUShort() = value

    companion object : HasWasmReader<WasmUShort> {
        const val SIZE_BYTES = 2
        val Zero = WasmUShort(0u)
        val Max = WasmUShort(UShort.MAX_VALUE)

        override val reader
            get() = object : WasmMemoryReader<WasmUShort> {
                override fun read(memory: WasmMemory, offset: Int) = run {
                    WasmUShort(memory.uint16(offset))
                }

                override val elementSize: Int = SIZE_BYTES
            }
    }
}

@OptIn(ExperimentalWasmApi::class)
@JvmInline
value class WasmShort(val value: Short) : WasmValue {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmS16" }
        buffer.writeInt16LE(value, offset)
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString() = value.toString()
    override fun toString(memory: WasmMemory) = toString()

    fun toShort() = value

    companion object : HasWasmReader<WasmShort> {
        const val SIZE_BYTES = 2
        val Zero = WasmShort(0)
        val Min = WasmShort(Short.MIN_VALUE)
        val Max = WasmShort(Short.MAX_VALUE)

        override val reader
            get() = object : WasmMemoryReader<WasmShort> {
                override fun read(memory: WasmMemory, offset: Int) = WasmShort(memory.int16(offset))
                override val elementSize: Int = SIZE_BYTES
            }
    }
}
