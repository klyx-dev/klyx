package com.klyx.extension.wasm

import com.klyx.wasm.utils.writeInt32LE

data class WasmResult(
    val isSuccess: Boolean,
    val dataPtr: Int,
    val dataLen: Int
) {
    companion object {
        fun ok(dataPtr: Int, dataLen: Int) = WasmResult(true, dataPtr, dataLen)
        fun err(errorPtr: Int, errorLen: Int) = WasmResult(false, errorPtr, errorLen)
    }

    /**
     * Write this result to a WASM memory buffer in little endian format
     * Result format: [success: i32][ptr: i32][len: i32]
     */
    fun writeToBuffer(buffer: ByteArray, offset: Int = 0) {
        require(offset + 12 <= buffer.size) { "Buffer too small for result" }
        buffer.writeInt32LE(if (isSuccess) 0 else 1, offset)
        buffer.writeInt32LE(dataPtr, offset + 4)
        buffer.writeInt32LE(dataLen, offset + 8)
    }

    fun toBuffer(): ByteArray = ByteArray(12).apply { writeToBuffer(this) }
}
