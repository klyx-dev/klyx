@file:Suppress("UnusedParameter")

package com.klyx.wasm.todo

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.expect
import io.github.charlietap.chasm.embedding.instance
import io.github.charlietap.chasm.embedding.shapes.Import
import io.github.charlietap.chasm.embedding.shapes.fold
import io.github.charlietap.chasm.embedding.store
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
    private val store = store()
    private lateinit var _module: WasmModule

    private val imports = mutableListOf<Import>()

    override fun close() {
        cancel("WASM scope closed")
    }

    @OptIn(ExperimentalContracts::class)
    fun module(block: WasmModuleBuilder.() -> Result<WasmModule, WasmException>) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        val result = WasmModuleBuilder().block()
        _module = result.expect { "Failed to load WASM module" }
    }

    internal fun import(import: Import) {
        imports += import
    }

    fun function(
        name: String,
        params: List<WasmType>,
        results: List<WasmType>,
        module: String = "env",
    ) {

    }

    @PublishedApi
    internal fun createInstance(): WasmInstance {
        check(::_module.isInitialized) { "WASM module not initialized. Did you forget to call `module { ... }`?" }

        val instance = instance(store, _module.module, imports).fold(
            onSuccess = { it },
            onError = { error("Failed to create WASM instance") }
        ).asWasmInstance()

        return instance
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
