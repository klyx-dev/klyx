@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.util

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.memory.uint8
import io.itsvks.anyhow.Err
import io.itsvks.anyhow.Ok
import io.itsvks.anyhow.Result

inline fun <T> WasmMemory.readOption(
    pointer: Int,
    readSome: WasmMemory.(offset: Int) -> T
): Option<T> {
    val tag = uint8(pointer)
    return if (tag.toInt() == 1) {
        Some(readSome(pointer + 4))
    } else {
        None
    }
}

fun WasmMemory.dumpBytes(start: Int, count: Int = 32): String {
    val sb = StringBuilder()
    for (i in 0 until count) {
        val b: UByte = uint8(start + i)
        sb.append(b.toHexString(format = HexFormat.UpperCase))
        sb.append(' ')
    }
    return sb.toString()
}

inline fun <T, E> WasmMemory.readResult(
    pointer: Int,
    readOk: WasmMemory.(offset: Int) -> T,
    readErr: WasmMemory.(offset: Int) -> E
): Result<T, E> {
    val tag = uint8(pointer)

//    println("Memory at result pointer:")
//    println(dumpBytes(pointer, 32))

    return if (tag.toInt() == 0) {
        Ok(readOk(pointer + 4))
    } else {
        Err(readErr(pointer + 4))
    }
}

fun WasmMemory.readStringOption(pointer: Int): Option<String> {
    return readOption(pointer, WasmMemory::readLengthPrefixedUtf8String)
}

fun WasmMemory.readStringResult(pointer: Int): Result<String, String> {
    return readResult(pointer, WasmMemory::readLengthPrefixedUtf8String, WasmMemory::readLengthPrefixedUtf8String)
}
