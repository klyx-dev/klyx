package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmAny
import com.klyx.wasm.WasmMemory

@ExperimentalWasmApi
interface WasmValue : WasmAny {
    /**
     * Creates a reader that can reconstruct instances of this type from memory
     */
    fun createReader(): WasmMemoryReader<out WasmValue>
}

@OptIn(ExperimentalWasmApi::class)
context(memory: WasmMemory)
fun <T> WasmValue(value: T): WasmValue = when (value) {
    is Boolean -> WasmBool(value)
    is UByte -> WasmUByte(value)
    is Byte -> WasmByte(value)
    is UShort -> WasmUShort(value)
    is Short -> WasmShort(value)
    is UInt -> WasmUInt(value)
    is Int -> WasmInt(value)
    is ULong -> WasmULong(value)
    is Long -> WasmLong(value)
    is Float -> WasmFloat(value)
    is Double -> WasmDouble(value)
    is Char -> WasmChar(value)
    is String -> WasmString(value)
    else -> throw IllegalArgumentException("Unsupported value type: ${value!!::class}")
}

fun Boolean.toWasm() = WasmBool(this)
fun UByte.toWasm() = WasmUByte(this)
fun Byte.toWasm() = WasmByte(this)
fun UShort.toWasm() = WasmUShort(this)
fun Short.toWasm() = WasmShort(this)
fun UInt.toWasm() = WasmUInt(this)
fun Int.toWasm() = WasmInt(this)
fun ULong.toWasm() = WasmULong(this)
fun Long.toWasm() = WasmLong(this)
fun Float.toWasm() = WasmFloat(this)
fun Double.toWasm() = WasmDouble(this)
fun Char.toWasm() = WasmChar(this)

// Needs WasmMemory in context for allocation
@OptIn(ExperimentalWasmApi::class)
context(memory: WasmMemory)
fun String.toWasm() = WasmString(this)
