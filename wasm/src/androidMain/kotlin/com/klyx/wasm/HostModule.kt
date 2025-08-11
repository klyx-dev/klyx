package com.klyx.wasm

import kotlinx.coroutines.CoroutineScope
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

@ExperimentalWasmApi
typealias HostFnSync = FunctionScope.(args: LongArray) -> LongArray?

@ExperimentalWasmApi
typealias HostFnSuspend = suspend FunctionScope.(args: LongArray) -> Unit

@ExperimentalWasmApi
interface HostModule {
    val name: String

    fun HostModuleScope.functions()
}

@WasmDsl
@ExperimentalWasmApi
class HostModuleScope @PublishedApi internal constructor(
    private val moduleName: ModuleName,
    private val wasmScope: WasmScope
) {

    infix operator fun WasmType.plus(other: WasmType): List<WasmType> = listOf(this, other)
    infix operator fun List<WasmType>.plus(other: WasmType): List<WasmType> = this + listOf(other)

    val i32 get() = WasmType.I32
    val i64 get() = WasmType.I64
    val f32 get() = WasmType.F32
    val f64 get() = WasmType.F64
    val string get() = WasmType.String

    fun function(
        name: FunctionName,
        signature: WasmSignature,
        implementation: HostFnSync
    ) = apply {
        with(wasmScope) {
            function(
                name = name,
                params = signature.params,
                results = signature.results,
                moduleName = this@HostModuleScope.moduleName,
                implementation = implementation
            )
        }
    }

    fun function(
        name: FunctionName,
        params: List<WasmType>,
        implementation: HostFnSuspend
    ) = apply {
        with(wasmScope) {
            function(name, params, this@HostModuleScope.moduleName, implementation)
        }
    }

    fun function(
        name: FunctionName,
        results: List<WasmType>,
        implementation: FunctionScope.() -> LongArray
    ) = apply {
        function(name, signature { returns(results) }) { implementation() }
    }

    fun function(
        name: FunctionName,
        implementation: suspend FunctionScope.() -> Unit
    ) = apply {
        with(wasmScope) {
            function(
                name,
                params = emptyList(),
                moduleName = this@HostModuleScope.moduleName
            ) { _ -> implementation() }
        }
    }
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
@WasmDsl
@ExperimentalWasmApi
inline fun WasmScope.hostModule(
    name: ModuleName,
    @BuilderInference
    block: HostModuleScope.() -> Unit
) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    HostModuleScope(name, this).apply(block)
}
