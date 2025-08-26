@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.wasm

import com.klyx.wasm.internal.InternalExperimentalWasmApi
import com.klyx.wasm.internal.asByte
import com.klyx.wasm.internal.asDouble
import com.klyx.wasm.internal.asFloat
import com.klyx.wasm.internal.asInt
import com.klyx.wasm.internal.asLong
import com.klyx.wasm.internal.asShort
import com.klyx.wasm.internal.asUInt
import com.klyx.wasm.internal.asULong
import com.klyx.wasm.type.WasmString
import io.github.charlietap.chasm.runtime.value.ExecutionValue
import io.github.charlietap.chasm.runtime.value.NumberValue
import kotlin.jvm.JvmInline

@Suppress("NOTHING_TO_INLINE")
@OptIn(InternalExperimentalWasmApi::class)
@JvmInline
@ExperimentalWasmApi
value class WasmValue(
    @PublishedApi
    internal val value: ExecutionValue
) {
    inline fun asByte() = value.asByte()
    inline fun asShort() = value.asShort()
    inline fun asInt() = value.asInt()
    inline fun asUInt() = value.asUInt()
    inline fun asLong() = value.asLong()
    inline fun asULong() = value.asULong()

    @InternalExperimentalWasmApi
    inline fun asFloat() = value.asFloat()

    @InternalExperimentalWasmApi
    inline fun asDouble() = value.asDouble()
}

fun Byte.toWasmValue() = WasmValue(NumberValue.I32(toInt()))
fun Short.toWasmValue() = WasmValue(NumberValue.I32(toInt()))
fun Int.toWasmValue() = WasmValue(NumberValue.I32(this))
fun UInt.toWasmValue() = WasmValue(NumberValue.I32(toInt()))
fun Long.toWasmValue() = WasmValue(NumberValue.I64(this))
fun ULong.toWasmValue() = WasmValue(NumberValue.I64(toLong()))

internal fun List<ExecutionValue>.toWasmValues() = map { WasmValue(it) }
internal fun List<WasmValue>.asExecutionValues() = map { it.value }
