package com.klyx.wasm.wasi

import com.dylibso.chicory.wasi.WasiPreview1
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmScope
import com.klyx.wasm.addFunction
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class, ExperimentalWasmApi::class)
@WasiDsl
@ExperimentalWasiApi
inline fun WasmScope.withWasi(
    @BuilderInference
    block: WasiScope.() -> Unit
) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val wasi = WasiPreview1.builder()
        .withOptions(WasiScope().apply(block).build())
        .withLogger(WasiLogger)
        .build()
    store.addFunction(*wasi.toHostFunctions())
}
