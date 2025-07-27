package com.klyx.wasm

import com.dylibso.chicory.runtime.ExportFunction

class WasmHostCallable internal constructor(
    private val function: ExportFunction
) {
    operator fun invoke(vararg args: Long): LongArray? = function.apply(*args)
}

internal fun ExportFunction.toWasmHostCallable() = WasmHostCallable(this)
