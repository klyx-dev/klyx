package com.klyx.wasm

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

@ExperimentalWasmApi
data class WasmSignature(
    val params: List<WasmType>,
    val results: List<WasmType>
)

@ExperimentalWasmApi
class WasmSignatureBuilder {
    val params = mutableListOf<WasmType>()
    val results = mutableListOf<WasmType>()

    val nothing = Unit

    infix fun WasmType.returns(result: WasmType): WasmSignature {
        return WasmSignature(params = listOf(this), results = listOf(result))
    }

    infix fun List<WasmType>.returns(result: WasmType): WasmSignature {
        return WasmSignature(params = this, results = listOf(result))
    }

    infix fun WasmType.returns(results: List<WasmType>): WasmSignature {
        return WasmSignature(params = listOf(this), results = results)
    }

    infix fun List<WasmType>.returns(results: List<WasmType>): WasmSignature {
        return WasmSignature(params = this, results = results)
    }

    infix fun List<WasmType>.returns(unit: Unit?): WasmSignature =
        WasmSignature(params = this, results = emptyList())

    infix fun WasmType.returns(unit: Unit): WasmSignature =
        WasmSignature(params = listOf(this), results = emptyList())

    infix fun returns(result: WasmType): WasmSignature =
        WasmSignature(params = emptyList(), results = listOf(result))

    infix fun returns(result: List<WasmType>): WasmSignature =
        WasmSignature(params = emptyList(), results = result)

    infix fun Unit.returns(result: WasmType): WasmSignature =
        WasmSignature(params = emptyList(), results = listOf(result))

    infix fun Unit.returns(results: List<WasmType>): WasmSignature =
        WasmSignature(params = emptyList(), results = results)

    infix fun Unit.returns(unit: Unit): WasmSignature =
        WasmSignature(params = emptyList(), results = emptyList())

    val none: WasmSignature
        get() = WasmSignature(params = emptyList(), results = emptyList())

    fun build() = WasmSignature(params, results)
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
@ExperimentalWasmApi
inline fun signature(
    @BuilderInference
    block: WasmSignatureBuilder.() -> WasmSignature
): WasmSignature {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return WasmSignatureBuilder().run(block)
}
