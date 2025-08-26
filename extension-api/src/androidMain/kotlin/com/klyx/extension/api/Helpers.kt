@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.api

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.memory.uint32

inline fun <T> WasmMemory.readOption(
    pointer: Int,
    readSome: WasmMemory.(offset: Int) -> T
): Option<T> {
    val tag = uint32(pointer)
    return if (tag == 0u) {
        None
    } else {
        Some(readSome(pointer + 4))
    }
}

inline fun <T, E> WasmMemory.readResult(
    pointer: Int,
    readOk: WasmMemory.(offset: Int) -> T,
    readErr: WasmMemory.(offset: Int) -> E
): Result<T, E> {
    val tag = uint32(pointer)
    return if (tag == 0u) {
        Ok(readOk(pointer + 4))
    } else {
        Err(readErr(pointer + 4))
    }
}

fun WasmMemory.readStringOption(pointer: Int) = readOption(pointer, WasmMemory::readCString)

fun WasmMemory.readStringResult(pointer: Int) = run {
    readResult(pointer, WasmMemory::readCString, WasmMemory::readCString)
}
