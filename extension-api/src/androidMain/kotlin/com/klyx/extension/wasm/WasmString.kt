package com.klyx.extension.wasm

import com.klyx.wasm.utils.readInt32LE
import com.klyx.wasm.utils.writeInt32LE

data class WasmString(
    val ptr: Int,
    val len: Int
) {
    companion object {
        const val SIZE = 8

        /** Read WasmString from a byte buffer at given offset */
        fun fromBuffer(buffer: ByteArray, offset: Int = 0): WasmString {
            require(offset + SIZE <= buffer.size) { "Buffer too small for WasmString" }
            val ptr = buffer.readInt32LE(offset)
            val len = buffer.readInt32LE(offset + 4)
            return WasmString(ptr, len)
        }
    }

    /** Write this WasmString to a byte buffer at given offset */
    fun writeToBuffer(buffer: ByteArray, offset: Int = 0) {
        require(offset + SIZE <= buffer.size) { "Buffer too small for WasmString" }
        buffer.writeInt32LE(ptr, offset)
        buffer.writeInt32LE(len, offset + 4)
    }

    /** Convert to a fresh byte buffer */
    fun toBuffer(): ByteArray = ByteArray(SIZE).also { writeToBuffer(it) }
}

typealias PointerLenPair = Pair<Int, Int>

fun WasmString(ptrLenPair: PointerLenPair) = WasmString(ptrLenPair.first, ptrLenPair.second)
fun PointerLenPair.toWasmString() = WasmString(this)
fun WasmString.toPointerLenPair() = Pair(ptr, len)
