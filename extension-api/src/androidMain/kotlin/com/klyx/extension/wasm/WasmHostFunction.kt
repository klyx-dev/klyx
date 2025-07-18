package com.klyx.extension.wasm

import kwasm.api.UnitHostFunction
import kwasm.runtime.IntValue

fun ((String) -> Unit).toHostFunction() = UnitHostFunction { ptr: IntValue, len: IntValue, ctx ->
    this(string(ptr, len, ctx))
}
