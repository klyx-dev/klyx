package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.internal.packInts
import com.klyx.wasm.internal.unpackInt1
import com.klyx.wasm.internal.unpackInt2
import com.klyx.wasm.utils.toBytesLE
import com.klyx.wasm.utils.writeInt32LE
import com.klyx.wasm.write

@OptIn(ExperimentalWasmApi::class)
@JvmInline
value class WasmString(private val packedValue: Long) : WasmValue {
    private val ptr get() = unpackInt1(packedValue)
    private val len get() = unpackInt2(packedValue)

    context(memory: WasmMemory)
    val value get() = memory.readString(ptr, len)

    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + SIZE_BYTES <= buffer.size) { "Buffer too small for WasmString" }
        buffer.writeInt32LE(ptr, offset)
        buffer.writeInt32LE(len, offset + 4)
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader

    override fun toString(): String {
        return "WasmString(ptr=$ptr, len=$len)"
    }

    override fun toString(memory: WasmMemory): String {
        return with(memory) { value }
    }

    companion object : HasWasmReader<WasmString> {
        const val SIZE_BYTES = 8
        val Empty = WasmString(0, 0)

        override val reader
            get() = object : WasmMemoryReader<WasmString> {
                override fun read(memory: WasmMemory, offset: Int): WasmString {
                    val ptr = memory.readI32(offset)
                    val len = memory.readI32(offset + 4)
                    return WasmString(ptr, len)
                }

                override val elementSize: Int = SIZE_BYTES
            }
    }
}

fun WasmString(ptr: Int, len: Int) = WasmString(packInts(ptr, len))

@OptIn(ExperimentalWasmApi::class)
private fun WasmMemory.writeString(value: String): WasmString {
    val (ptr, len) = write(value.toBytesLE())
    return WasmString(ptr, len)
}

@OptIn(ExperimentalWasmApi::class)
context(memory: WasmMemory)
fun WasmString(value: String) = memory.writeString(value)

