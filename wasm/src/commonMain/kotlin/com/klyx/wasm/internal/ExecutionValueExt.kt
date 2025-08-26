@file:Suppress("NOTHING_TO_INLINE")

package com.klyx.wasm.internal

import io.github.charlietap.chasm.runtime.value.ExecutionValue
import io.github.charlietap.chasm.runtime.value.NumberValue

@InternalExperimentalWasmApi
inline fun ExecutionValue.asByte(): Byte = (this as NumberValue.I32).value.toByte()

@InternalExperimentalWasmApi
inline fun ExecutionValue.asShort(): Short = (this as NumberValue.I32).value.toShort()

@InternalExperimentalWasmApi
inline fun ExecutionValue.asInt(): Int = (this as NumberValue.I32).value

@InternalExperimentalWasmApi
inline fun ExecutionValue.asUInt(): UInt = (this as NumberValue.I32).value.toUInt()

@InternalExperimentalWasmApi
inline fun ExecutionValue.asLong(): Long = (this as NumberValue.I64).value

@InternalExperimentalWasmApi
inline fun ExecutionValue.asULong(): ULong = (this as NumberValue.I64).value.toULong()

@InternalExperimentalWasmApi
inline fun ExecutionValue.asFloat(): Float = Float.fromBits(asInt())

@InternalExperimentalWasmApi
inline fun ExecutionValue.asDouble(): Double = Double.fromBits(asLong())

@InternalExperimentalWasmApi
inline fun Byte.asExecutionValue(): ExecutionValue = NumberValue.I32(this.toInt())

@InternalExperimentalWasmApi
inline fun Short.asExecutionValue(): ExecutionValue = NumberValue.I32(this.toInt())

@InternalExperimentalWasmApi
inline fun Int.asExecutionValue(): ExecutionValue = NumberValue.I32(this)

@InternalExperimentalWasmApi
inline fun UInt.asExecutionValue(): ExecutionValue = NumberValue.I32(this.toInt())

@InternalExperimentalWasmApi
inline fun Long.asExecutionValue(): ExecutionValue = NumberValue.I64(this)

@InternalExperimentalWasmApi
inline fun ULong.asExecutionValue(): ExecutionValue = NumberValue.I64(this.toLong())

@InternalExperimentalWasmApi
inline fun Float.asExecutionValue(): ExecutionValue = NumberValue.F32(this)

@InternalExperimentalWasmApi
inline fun Double.asExecutionValue(): ExecutionValue = NumberValue.F64(this)


