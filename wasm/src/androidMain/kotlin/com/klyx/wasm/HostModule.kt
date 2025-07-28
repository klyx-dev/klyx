package com.klyx.wasm

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

typealias HostFnSync = (WasmInstance, args: LongArray) -> LongArray?
typealias HostFnSuspend = suspend (WasmInstance, args: LongArray) -> Unit

interface HostModule {
    val name: String

    fun HostModuleScope.functions()
}

@WasmDsl
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
    val string get() = WasmType.String

    fun function(
        name: String,
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
        name: String,
        signature: WasmSignature,
        implementation: (args: LongArray) -> LongArray?
    ) = apply {
        function(name, signature) { _, args -> implementation(args) }
    }

    fun function(
        name: String,
        params: List<WasmType>,
        implementation: HostFnSuspend
    ) = apply {
        with(wasmScope) {
            function(name, params, this@HostModuleScope.moduleName, implementation)
        }
    }

    fun function(
        name: String,
        params: List<WasmType>,
        implementation: suspend (args: LongArray) -> Unit
    ) = apply {
        function(name, params) { _, args -> implementation(args) }
    }

    fun function(
        name: String,
        results: List<WasmType>,
        implementation: () -> LongArray
    ) = apply {
        function(name, signature { returns(results) }) { implementation() }
    }

    fun function(
        name: String,
        results: List<WasmType>,
        implementation: (WasmInstance) -> LongArray
    ) = apply {
        function(name, signature { returns(results) }) { instance, _ ->
            implementation(instance)
        }
    }

    fun function(
        name: String,
        implementation: suspend () -> Unit
    ) = apply {
        with(wasmScope) {
            function(
                name,
                params = emptyList(),
                moduleName = this@HostModuleScope.moduleName
            ) { _, _ ->
                implementation()
            }
        }
    }

    fun function(
        name: String,
        implementation: HostFnSync
    ) = apply {
        with(wasmScope) {
            function(
                name,
                params = emptyList(),
                results = emptyList(),
                moduleName = this@HostModuleScope.moduleName,
                implementation
            )
        }
    }
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
@WasmDsl
inline fun WasmScope.hostModule(
    name: String,
    @BuilderInference
    block: HostModuleScope.() -> Unit
) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    HostModuleScope(name, this).apply(block)
}
