package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.internal.writeFloat64LittleEndian
import com.klyx.wasm.internal.writeInt64LittleEndian
import com.klyx.wasm.internal.writeUInt64LittleEndian
import com.klyx.wasm.memory.float64
import com.klyx.wasm.memory.int64
import com.klyx.wasm.memory.uint64
import kotlin.jvm.JvmInline

@OptIn(ExperimentalWasmApi::class)
@JvmInline
value class WasmULong(val value: ULong) : WasmType {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmU64" }
        buffer.writeUInt64LittleEndian(value, offset)
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString() = value.toString()
    override fun toString(memory: WasmMemory) = toString()

    fun toULong() = value

    companion object : HasWasmReader<WasmULong> {
        const val SIZE_BYTES = 8
        val Zero = WasmULong(0u)
        val Max = WasmULong(ULong.MAX_VALUE)

        override val reader
            get() = object : WasmMemoryReader<WasmULong> {
                override fun read(memory: WasmMemory, offset: Int): WasmULong {
                    return WasmULong(memory.uint64(offset))
                }

                override val elementSize: Int = SIZE_BYTES
            }
    }
}

@OptIn(ExperimentalWasmApi::class)
@JvmInline
value class WasmLong(val value: Long) : WasmType {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmS64" }
        buffer.writeInt64LittleEndian(value, offset)
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString() = value.toString()
    override fun toString(memory: WasmMemory) = toString()

    fun toLong() = value

    companion object : HasWasmReader<WasmLong> {
        const val SIZE_BYTES = 8
        val Zero = WasmLong(0L)
        val Min = WasmLong(Long.MIN_VALUE)
        val Max = WasmLong(Long.MAX_VALUE)

        override val reader
            get() = object : WasmMemoryReader<WasmLong> {
                override fun read(memory: WasmMemory, offset: Int) = WasmLong(memory.int64(offset))
                override val elementSize: Int = SIZE_BYTES
            }
    }
}

@OptIn(ExperimentalWasmApi::class)
@JvmInline
value class WasmDouble(val value: Double) : WasmType {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmF64" }
        buffer.writeFloat64LittleEndian(value, offset)
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString() = value.toString()
    override fun toString(memory: WasmMemory) = toString()

    fun toDouble() = value

    companion object : HasWasmReader<WasmDouble> {
        const val SIZE_BYTES = 8
        val Zero = WasmDouble(0.0)
        val NaN = WasmDouble(Double.NaN)
        val PositiveInfinity = WasmDouble(Double.POSITIVE_INFINITY)
        val NegativeInfinity = WasmDouble(Double.NEGATIVE_INFINITY)

        override val reader
            get() = object : WasmMemoryReader<WasmDouble> {
                override fun read(memory: WasmMemory, offset: Int): WasmDouble {
                    return WasmDouble(memory.float64(offset))
                }

                override val elementSize: Int = SIZE_BYTES
            }
    }
}
