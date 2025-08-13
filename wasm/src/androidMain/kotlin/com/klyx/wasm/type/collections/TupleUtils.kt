@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.wasm.type.collections

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.WasmValue

fun <A : WasmValue, B : WasmValue> Tuple2(a: A, b: B) = Tuple2(a to b)
fun <A : WasmValue, B : WasmValue, C : WasmValue> Tuple3(a: A, b: B, c: C) = Tuple3(Triple(a, b, c))

context(memory: WasmMemory)
fun <A, B> Pair<A, B>.toWasmTuple() = Tuple2(WasmValue(first), WasmValue(second))

context(memory: WasmMemory)
fun <A, B, C> Triple<A, B, C>.toWasmTuple() =
    Tuple3(WasmValue(first), WasmValue(second), WasmValue(third))

fun <A : WasmValue, B : WasmValue> Pair<A, B>.toTuple() = Tuple2(this)
fun <A : WasmValue, B : WasmValue, C : WasmValue> Triple<A, B, C>.toTuple() = Tuple3(this)

infix fun <A : WasmValue, B : WasmValue> A.with(other: B) = Tuple2(this to other)

fun <A : WasmValue, B : WasmValue, C : WasmValue> tupleOf(
    first: A, second: B, third: C
) = Tuple3(first, second, third)

// destructuring support
operator fun <A : WasmValue, B : WasmValue> Tuple2<A, B>.component1(): A = first
operator fun <A : WasmValue, B : WasmValue> Tuple2<A, B>.component2(): B = second

operator fun <A : WasmValue, B : WasmValue, C : WasmValue> Tuple3<A, B, C>.component1(): A = first
operator fun <A : WasmValue, B : WasmValue, C : WasmValue> Tuple3<A, B, C>.component2(): B = second
operator fun <A : WasmValue, B : WasmValue, C : WasmValue> Tuple3<A, B, C>.component3(): C = third
