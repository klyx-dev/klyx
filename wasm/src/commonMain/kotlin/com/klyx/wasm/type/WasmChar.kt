package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.internal.writeInt32LittleEndian
import kotlin.jvm.JvmInline

@OptIn(ExperimentalWasmApi::class)
@JvmInline
value class WasmChar(val value: Char) : WasmType {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmChar" }
        buffer.writeInt32LittleEndian(value.code, offset) // UTF-16 code unit stored in i32
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString() = value.toString()
    override fun toString(memory: WasmMemory) = toString()

    fun toChar() = value

    companion object : HasWasmReader<WasmChar> {
        const val SIZE_BYTES = 4
        val Null = WasmChar('\u0000')

        override val reader
            get() = object : WasmMemoryReader<WasmChar> {
                override fun read(memory: WasmMemory, offset: Int): WasmChar {
                    val codePoint = memory.readInt(offset)
                    return WasmChar(codePoint.toChar())
                }

                override val elementSize: Int = SIZE_BYTES
            }
    }
}
