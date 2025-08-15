package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmAny
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.collections.Tuple2
import com.klyx.wasm.type.collections.Tuple3
import com.klyx.wasm.type.collections.WasmList
import com.klyx.wasm.type.collections.toWasmList

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

typealias bool = WasmBool
typealias uint8 = WasmUByte
typealias int8 = WasmByte
typealias uint16 = WasmUShort
typealias int16 = WasmShort
typealias uint32 = WasmUInt
typealias int32 = WasmInt
typealias uint64 = WasmULong
typealias int64 = WasmLong
typealias float32 = WasmFloat
typealias float64 = WasmDouble
typealias char = WasmChar
typealias string = WasmString

typealias list<T> = WasmList<T>
typealias tuple2<A, B> = Tuple2<A, B>
typealias tuple3<A, B, C> = Tuple3<A, B, C>

typealias Vec<T> = list<T>

typealias u8 = uint8
typealias i8 = int8
typealias u16 = uint16
typealias i16 = int16
typealias u32 = uint32
typealias i32 = int32
typealias u64 = uint64
typealias i64 = int64
typealias f32 = float32
typealias f64 = float64
typealias c16 = char
typealias str = string

// Needs WasmMemory in context for allocation
@OptIn(ExperimentalWasmApi::class)
context(memory: WasmMemory)
fun String.toWasm() = WasmString(this)

@OptIn(ExperimentalWasmApi::class)
context(memory: WasmMemory)
fun ByteArray.toWasm() = map { it.toWasm() }.toWasmList()

@OptIn(ExperimentalWasmApi::class, ExperimentalUnsignedTypes::class)
context(memory: WasmMemory)
fun UByteArray.toWasm() = map { it.toWasm() }.toWasmList()
