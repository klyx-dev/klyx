@file:OptIn(ExperimentalWasmApi::class)
@file:Suppress("NOTHING_TO_INLINE")

package com.klyx.wasm.memory

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory

inline fun WasmMemory.int8(pointer: Int) = readByte(pointer)
inline fun WasmMemory.uint8(pointer: Int) = readByte(pointer).toUByte()

inline fun WasmMemory.int16(pointer: Int) = readInt(pointer).toShort()
inline fun WasmMemory.uint16(pointer: Int) = readInt(pointer).toUShort()

inline fun WasmMemory.int32(pointer: Int) = readInt(pointer)
inline fun WasmMemory.uint32(pointer: Int) = readInt(pointer).toUInt()

inline fun WasmMemory.int64(pointer: Int) = readLong(pointer)
inline fun WasmMemory.uint64(pointer: Int) = readLong(pointer).toULong()

inline fun WasmMemory.float32(pointer: Int) = readFloat(pointer)
inline fun WasmMemory.float64(pointer: Int) = readDouble(pointer)

