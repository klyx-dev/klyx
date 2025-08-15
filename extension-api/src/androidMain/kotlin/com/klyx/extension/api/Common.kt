package com.klyx.extension.api

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmAny
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.i32
import com.klyx.wasm.type.list
import com.klyx.wasm.type.str
import com.klyx.wasm.type.tuple2

typealias EnvVars = list<tuple2<str, str>>

/**
 * @property start The start of the range (inclusive).
 * @property end The end of the range (exclusive).
 */
@OptIn(ExperimentalWasmApi::class)
data class Range(val start: i32, val end: i32) : WasmAny {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        start.writeToBuffer(buffer, offset)
        end.writeToBuffer(buffer, offset + start.sizeInBytes())
    }

    override fun sizeInBytes(): Int {
        return start.sizeInBytes() + end.sizeInBytes()
    }

    override fun toString(memory: WasmMemory): String {
        return "Range(start=${start.toString(memory)}, end=${end.toString(memory)})"
    }
}
