package com.klyx.wasm.wasi

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmScope
import com.klyx.wasm.addFunction
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class, ExperimentalWasmApi::class)
@WasiDsl
@ExperimentalWasiApi
inline fun WasmScope.withWasi(
    block: WasiScope.() -> Unit
) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

    val wasiImports = WasiScope().apply(block).build()
    store.addFunction(*wasiImports.toTypedArray())
}
