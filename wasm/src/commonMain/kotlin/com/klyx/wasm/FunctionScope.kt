package com.klyx.wasm

import com.klyx.wasm.internal.InternalExperimentalWasmApi

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

    @OptIn(InternalExperimentalWasmApi::class)
    inline fun takeFloat() = take().asFloat()

    @OptIn(InternalExperimentalWasmApi::class)
    inline fun takeDouble() = take().asDouble()
}

