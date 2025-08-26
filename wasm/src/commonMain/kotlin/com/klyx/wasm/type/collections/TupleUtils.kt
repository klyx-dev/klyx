@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.wasm.type.collections

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.WasmType

fun <A : WasmType, B : WasmType> Tuple2(a: A, b: B) = Tuple2(a to b)
fun <A : WasmType, B : WasmType, C : WasmType> Tuple3(a: A, b: B, c: C) = Tuple3(Triple(a, b, c))

context(memory: WasmMemory)
fun <A, B> Pair<A, B>.toWasmTuple() = Tuple2(WasmType(first), WasmType(second))

context(memory: WasmMemory)
fun <A, B, C> Triple<A, B, C>.toWasmTuple() =
    Tuple3(WasmType(first), WasmType(second), WasmType(third))

fun <A : WasmType, B : WasmType> Pair<A, B>.toTuple() = Tuple2(this)
fun <A : WasmType, B : WasmType, C : WasmType> Triple<A, B, C>.toTuple() = Tuple3(this)

infix fun <A : WasmType, B : WasmType> A.with(other: B) = Tuple2(this to other)

fun <A : WasmType, B : WasmType, C : WasmType> tupleOf(
    first: A, second: B, third: C
) = Tuple3(first, second, third)
