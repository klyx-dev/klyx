package com.klyx.extension.wasm

import com.klyx.wasm.internal.writeInt32LittleEndian

data class WasmOption(
    val isSome: Boolean,
    val dataPtr: Int = 0,
    val dataLen: Int = 0
) {
    companion object {
        fun some(dataPtr: Int, dataLen: Int) = WasmOption(true, dataPtr, dataLen)
        fun none() = WasmOption(false, 0, 0)
    }

    /**
     * Write this option to a WASM memory buffer in little endian format
     * Option format: [discriminant: i32][pointer: i32][length: i32]
     * discriminant = 0 for None, 1 for Some
     */
    fun writeToBuffer(buffer: ByteArray, offset: Int = 0) {
        require(offset + 12 <= buffer.size) { "Buffer too small for option" }
        buffer.writeInt32LittleEndian(if (isSome) 1 else 0, offset) // discriminant
        buffer.writeInt32LittleEndian(dataPtr, offset + 4)
        buffer.writeInt32LittleEndian(dataLen, offset + 8)
    }

    fun toBuffer(): ByteArray = ByteArray(12).apply { writeToBuffer(this) }
}
