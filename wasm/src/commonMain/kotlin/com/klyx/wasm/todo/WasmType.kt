package com.klyx.wasm.todo

import io.github.charlietap.chasm.type.NumberType
import io.github.charlietap.chasm.type.ValueType

@ExperimentalWasmApi
enum class WasmType(internal vararg val type: ValueType) {
    I32(ValueType.Number(NumberType.I32)),
    I64(ValueType.Number(NumberType.I64)),
    F32(ValueType.Number(NumberType.F32)),
    F64(ValueType.Number(NumberType.F64)),

    String(ValueType.Number(NumberType.I32), ValueType.Number(NumberType.I32)),
}
