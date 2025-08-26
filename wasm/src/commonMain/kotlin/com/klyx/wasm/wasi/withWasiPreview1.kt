package com.klyx.wasm.wasi

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmScope
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@ExperimentalWasiApi
@WasiDsl
@OptIn(ExperimentalWasmApi::class, ExperimentalContracts::class)
inline fun WasmScope.withWasiPreview1(block: WasiScope.() -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val imports = WasiScope(store).apply(block).build()
    addImports(imports)
}
