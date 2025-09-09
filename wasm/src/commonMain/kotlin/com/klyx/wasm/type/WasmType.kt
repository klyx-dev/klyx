package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.collections.Tuple2
import com.klyx.wasm.type.collections.Tuple3
import com.klyx.wasm.type.collections.WasmList
import com.klyx.wasm.type.collections.toWasmList

@ExperimentalWasmApi
interface WasmType : WasmAny {
    /**
     * Creates a reader that can reconstruct instances of this type from memory
     */
    fun createReader(): WasmMemoryReader<out WasmType>
}

@OptIn(ExperimentalWasmApi::class)
context(memory: WasmMemory)
fun <T> WasmType(value: T): WasmType = when (value) {
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
    is Unit -> WasmUnit
    else -> throw IllegalArgumentException("Unsupported value type: ${value!!::class}")
}

@Suppress("UnusedReceiverParameter")
val Unit.wasm get() = WasmUnit
val Boolean.wasm get() = WasmBool(this)
val UByte.wasm get() = WasmUByte(this)
val Byte.wasm get() = WasmByte(this)
val UShort.wasm get() = WasmUShort(this)
val Short.wasm get() = WasmShort(this)
val UInt.wasm get() = WasmUInt(this)
val Int.wasm get() = WasmInt(this)
val ULong.wasm get() = WasmULong(this)
val Long.wasm get() = WasmLong(this)
val Float.wasm get() = WasmFloat(this)
val Double.wasm get() = WasmDouble(this)
val Char.wasm get() = WasmChar(this)

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
val String.wstr get() = this.toWasm()

@OptIn(ExperimentalWasmApi::class)
context(memory: WasmMemory)
val ByteArray.wasm get() = map { it.wasm }.toWasmList()

@OptIn(ExperimentalWasmApi::class, ExperimentalUnsignedTypes::class)
context(memory: WasmMemory)
val UByteArray.wasm get() = map { it.wasm }.toWasmList()
