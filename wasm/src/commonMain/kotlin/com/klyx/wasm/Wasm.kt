package com.klyx.wasm

import arrow.core.None
import arrow.core.Option
import arrow.core.some
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.expect
import com.klyx.wasm.internal.InternalExperimentalWasmApi
import io.github.charlietap.chasm.embedding.function
import io.github.charlietap.chasm.embedding.instance
import io.github.charlietap.chasm.embedding.shapes.Import
import io.github.charlietap.chasm.embedding.shapes.Importable
import io.github.charlietap.chasm.embedding.store
import io.github.charlietap.chasm.type.FunctionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@DslMarker
internal annotation class WasmDsl

internal val WasmCoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

@WasmDsl
@ExperimentalWasmApi
class WasmScope @PublishedApi internal constructor() : CoroutineScope by WasmCoroutineScope, AutoCloseable {
    @PublishedApi
    internal val store = store()
    private var _module: Option<WasmModule> = None

    private val imports = mutableListOf<Import>()

    override fun close() {
        cancel("WASM scope closed")
    }

    @OptIn(ExperimentalContracts::class)
    fun module(block: WasmModuleBuilder.() -> Result<WasmModule, WasmException>) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        val result = WasmModuleBuilder().block()
        _module = result.expect { "Failed to load WASM module" }.some()
    }

    internal fun import(
        moduleName: String,
        entityName: String,
        value: Importable
    ) = apply {
        imports += Import(moduleName, entityName, value)
    }

    @PublishedApi
    internal fun addImports(imports: List<Import>) = apply {
        this.imports += imports
    }

    @OptIn(InternalExperimentalWasmApi::class)
    fun hostFunction(
        name: String,
        params: List<WasmType>,
        results: List<WasmType>,
        moduleName: String = "env",
        function: FunctionScope.(List<WasmValue>) -> List<WasmValue>
    ) = apply {
        val type = FunctionType(
            params = params.asResultType(),
            results = results.asResultType(),
        )

        val function = function(store, type) {
            FunctionScope(instance.asWasmInstance(this.store), it.toWasmValues()).run {
                function(args).asExecutionValues()
            }
        }

        import(moduleName, name, function)
    }

    fun hostFunction(
        name: String,
        params: List<WasmType>,
        moduleName: String = "env",
        function: FunctionScope.(List<WasmValue>) -> Unit
    ) = apply {
        hostFunction(name, params, emptyList(), moduleName) { args ->
            function(args)
            emptyList()
        }
    }

    fun hostFunction(
        name: String,
        results: List<WasmType>,
        moduleName: String = "env",
        function: FunctionScope.() -> List<WasmValue>
    ) = apply {
        hostFunction(name, emptyList(), results, moduleName) { function() }
    }

    fun hostFunction(
        name: String,
        moduleName: String = "env",
        function: FunctionScope.() -> Unit
    ) = apply {
        hostFunction(name, emptyList(), emptyList(), moduleName) {
            function()
            emptyList()
        }
    }

    @PublishedApi
    internal fun createInstance(): WasmInstance {
        check(_module.isSome()) { "WASM module not initialized. Did you forget to call `module { ... }`?" }

        _module.onSome { some ->
            return instance(store, some.module, imports).asWasmInstance(store)
        }

        error(
            "Failed to create WASM instance, module not initialized. " +
                    "This could be due to a variety of reasons, such as a corrupted " +
                    "module file, incorrect module path, or issues with the WebAssembly runtime " +
                    "environment. Please ensure the module is correctly configured and accessible."
        )
    }
}

@OptIn(ExperimentalContracts::class)
@ExperimentalWasmApi
inline fun wasm(
    block: WasmScope.() -> Unit
): WasmInstance {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return WasmScope().apply(block).createInstance()
}
