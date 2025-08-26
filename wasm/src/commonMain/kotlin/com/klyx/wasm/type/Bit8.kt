package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.internal.toUtf8ByteArray
import com.klyx.wasm.internal.toUtf8UByteArray
import com.klyx.wasm.memory.int8
import com.klyx.wasm.memory.uint8
import kotlin.jvm.JvmInline

@OptIn(ExperimentalWasmApi::class)
@JvmInline
value class WasmUByte(val value: UByte) : WasmType {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmU8" }
        buffer[offset] = value.toByte()
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString() = value.toString()
    override fun toString(memory: WasmMemory) = toString()

    fun toUByte() = value
    fun toByte() = value.toByte()

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
value class WasmByte(val value: Byte) : WasmType {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmS8" }
        buffer[offset] = value
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString() = value.toString()
    override fun toString(memory: WasmMemory) = toString()

    fun toByte() = value
    fun toUByte() = value.toUByte()

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

fun String.toWasmUByte() = WasmUByte(toUByte())
fun String.toWasmByte() = WasmByte(toByte())

@OptIn(ExperimentalWasmApi::class, ExperimentalUnsignedTypes::class)
context(memory: WasmMemory)
fun String.asWasmU8Array() = toUtf8UByteArray().wasm

@OptIn(ExperimentalWasmApi::class)
context(memory: WasmMemory)
fun String.asWasmS8Array() = toUtf8ByteArray().wasm
