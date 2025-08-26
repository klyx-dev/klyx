package com.klyx.wasm

import io.github.charlietap.chasm.type.NumberType
import io.github.charlietap.chasm.type.ResultType
import io.github.charlietap.chasm.type.ValueType

@ExperimentalWasmApi
enum class WasmType(internal vararg val type: ValueType) {
    I32(ValueType.Number(NumberType.I32)),
    I64(ValueType.Number(NumberType.I64)),
    F32(ValueType.Number(NumberType.F32)),
    F64(ValueType.Number(NumberType.F64)),

    Utf8String(ValueType.Number(NumberType.I32), ValueType.Number(NumberType.I32)),
}

@OptIn(ExperimentalWasmApi::class)
internal fun WasmType.asResultType() = ResultType(type.asList())

@OptIn(ExperimentalWasmApi::class)
internal fun List<WasmType>.asResultType() = ResultType(flatMap { it.type.asList() })
