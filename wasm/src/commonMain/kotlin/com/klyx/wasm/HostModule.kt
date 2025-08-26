package com.klyx.wasm

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@ExperimentalWasmApi
interface HostModule {
    val name: String

    fun HostModuleScope.functions()
}

@WasmDsl
@ExperimentalWasmApi
class HostModuleScope @PublishedApi internal constructor(
    private val moduleName: String,
    private val wasmScope: WasmScope
) {
    infix operator fun WasmType.plus(other: WasmType): List<WasmType> = listOf(this, other)
    infix operator fun List<WasmType>.plus(other: WasmType): List<WasmType> = this + listOf(other)

    val i32 get() = WasmType.I32
    val i64 get() = WasmType.I64
    val f32 get() = WasmType.F32
    val f64 get() = WasmType.F64
    val string get() = WasmType.Utf8String

    fun function(
        name: String,
        signature: WasmSignature,
        implementation: FunctionScope.(List<WasmValue>) -> List<WasmValue>
    ) = apply {
        with(wasmScope) {
            hostFunction(
                name = name,
                params = signature.params,
                results = signature.results,
                moduleName = this@HostModuleScope.moduleName,
                function = implementation
            )
        }
    }

    fun function(
        name: String,
        params: List<WasmType>,
        implementation: FunctionScope.(List<WasmValue>) -> Unit
    ) = apply {
        with(wasmScope) {
            hostFunction(name, params, this@HostModuleScope.moduleName, implementation)
        }
    }

    fun function(
        name: String,
        results: List<WasmType>,
        implementation: FunctionScope.() -> List<WasmValue>
    ) = apply {
        function(name, signature { returns(results) }) { implementation() }
    }

    fun function(
        name: String,
        implementation: FunctionScope.() -> Unit
    ) = apply {
        with(wasmScope) {
            hostFunction(
                name,
                params = emptyList(),
                moduleName = this@HostModuleScope.moduleName
            ) {
                implementation()
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
@WasmDsl
@ExperimentalWasmApi
inline fun WasmScope.hostModule(
    name: String,
    block: HostModuleScope.() -> Unit
) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    HostModuleScope(name, this).apply(block)
}
