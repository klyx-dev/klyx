@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.wasm.type

import com.klyx.pointer.Pointer
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.readLoweredString
import com.klyx.wasm.utils.i32

inline fun <T : WasmValue> WasmMemory.readOption(
    pointer: Pointer,
    readSome: WasmMemory.(offset: Int) -> T
): Option<T> {
    val offset = pointer.raw.i32
    val tag = uint32(offset)
    return if (tag == 0u) {
        Option.None
    } else {
        Option.Some(readSome(offset + 4))
    }
}

inline fun <T : WasmValue, E : WasmValue> WasmMemory.readResult(
    pointer: Pointer,
    readOk: WasmMemory.(offset: Int) -> T,
    readErr: WasmMemory.(offset: Int) -> E
): Result<T, E> {
    val offset = pointer.raw.i32
    val tag = uint32(offset)
    return if (tag == 0u) {
        Result.Ok(readOk(offset + 4))
    } else {
        Result.Err(readErr(offset + 4))
    }
}

fun WasmMemory.readStringOption(pointer: Pointer) = readOption(pointer) {
    readLoweredString(it).toWasm()
}

fun WasmMemory.readStringResult(pointer: Pointer) = readResult(
    pointer = pointer,
    readOk = { readLoweredString(it).toWasm() },
    readErr = { readLoweredString(it).toWasm() },
)
