package com.klyx.wasm

import com.klyx.wasm.internal.InternalExperimentalWasmApi
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Suppress("NOTHING_TO_INLINE")
@ExperimentalWasmApi
class FunctionScope internal constructor(
    val instance: WasmInstance,
    val args: List<WasmValue>
) {
    val memory get() = instance.memory

    private var argsIdx = 0

    fun take() = args[argsIdx++]

    inline fun takeByte() = take().asByte()
    inline fun takeShort() = take().asShort()
    inline fun takeInt() = take().asInt()
    inline fun takeUInt() = take().asUInt()
    inline fun takeLong() = take().asLong()
    inline fun takeULong() = take().asULong()

    inline fun takeBoolean() = takeInt() != 0

    @OptIn(InternalExperimentalWasmApi::class)
    inline fun takeFloat() = take().asFloat()

    @OptIn(InternalExperimentalWasmApi::class)
    inline fun takeDouble() = take().asDouble()
}

@OptIn(ExperimentalWasmApi::class, ExperimentalContracts::class)
inline fun <R> FunctionScope.withMemory(block: WasmMemory.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return memory.block()
}
