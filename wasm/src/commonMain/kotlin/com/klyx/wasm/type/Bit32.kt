package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.internal.writeFloat32LittleEndian
import com.klyx.wasm.internal.writeInt32LittleEndian
import com.klyx.wasm.internal.writeUInt32LittleEndian
import com.klyx.wasm.memory.float32
import com.klyx.wasm.memory.int32
import com.klyx.wasm.memory.uint32
import kotlin.jvm.JvmInline

@OptIn(ExperimentalWasmApi::class)
@JvmInline
value class WasmUInt(val value: UInt) : WasmType {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmU32" }
        buffer.writeUInt32LittleEndian(value, offset)
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString() = value.toString()
    override fun toString(memory: WasmMemory) = toString()

    fun toUInt() = value

    companion object : HasWasmReader<WasmUInt> {
        const val SIZE_BYTES = 4
        val Zero = WasmUInt(0u)
        val Max = WasmUInt(UInt.MAX_VALUE)

        override val reader
            get() = object : WasmMemoryReader<WasmUInt> {
                override fun read(memory: WasmMemory, offset: Int) = WasmUInt(memory.uint32(offset))
                override val elementSize: Int = SIZE_BYTES
            }
    }
}

@OptIn(ExperimentalWasmApi::class)
@JvmInline
value class WasmInt(val value: Int) : WasmType {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmS32" }
        buffer.writeInt32LittleEndian(value, offset)
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString() = value.toString()
    override fun toString(memory: WasmMemory) = toString()

    fun toInt() = value

    companion object : HasWasmReader<WasmInt> {
        const val SIZE_BYTES = 4
        val Zero = WasmInt(0)
        val Min = WasmInt(Int.MIN_VALUE)
        val Max = WasmInt(Int.MAX_VALUE)

        override val reader
            get() = object : WasmMemoryReader<WasmInt> {
                override fun read(memory: WasmMemory, offset: Int) = WasmInt(memory.int32(offset))
                override val elementSize: Int = SIZE_BYTES
            }
    }
}

@OptIn(ExperimentalWasmApi::class)
@JvmInline
value class WasmFloat(val value: Float) : WasmType {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmF32" }
        buffer.writeFloat32LittleEndian(value, offset)
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString() = value.toString()
    override fun toString(memory: WasmMemory) = toString()

    fun toFloat() = value

    companion object : HasWasmReader<WasmFloat> {
        const val SIZE_BYTES = 4
        val Zero = WasmFloat(0.0f)
        val NaN = WasmFloat(Float.NaN)
        val PositiveInfinity = WasmFloat(Float.POSITIVE_INFINITY)
        val NegativeInfinity = WasmFloat(Float.NEGATIVE_INFINITY)

        override val reader
            get() = object : WasmMemoryReader<WasmFloat> {
                override fun read(memory: WasmMemory, offset: Int): WasmFloat {
                    return WasmFloat(memory.float32(offset))
                }

                override val elementSize: Int = SIZE_BYTES
            }
    }
}
