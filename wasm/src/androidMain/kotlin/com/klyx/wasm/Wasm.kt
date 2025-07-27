package com.klyx.wasm

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Store
import com.dylibso.chicory.wasi.WasiPreview1
import com.dylibso.chicory.wasm.Parser
import com.dylibso.chicory.wasm.types.FunctionType
import com.dylibso.chicory.wasm.types.ValType
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

enum class WasmType(internal val valType: ValType) {
    I32(ValType.I32),
    I64(ValType.I64),
    F32(ValType.F32),
    F64(ValType.F64)
}

@DslMarker
internal annotation class WasmDsl

@WasmDsl
class WasmScope : AutoCloseable {
    private val scope = MainScope()
    private val store = Store()
    private var callInit = false
    private var initFunction = "init"

    private lateinit var _module: WasmModule

    init {
        val wasi = WasiPreview1
            .builder()
            .withOptions(wasiOptions { inheritSystem() })
            .withLogger(WasiLogger)
            .build()
        store.addFunction(*wasi.toHostFunctions())
    }

    @OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
    fun module(
        @BuilderInference
        block: WasmModuleScope.() -> WasmModule
    ) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        _module = block(WasmModuleScope())
    }

    fun callInit(enabled: Boolean = true, function: String = "init") {
        this.callInit = enabled
        this.initFunction = function
    }

    fun function(
        name: String,
        params: List<WasmType>,
        results: List<WasmType>,
        namespace: String = "env",
        implementation: (WasmInstance, args: LongArray) -> LongArray?
    ) {
        val _params = params.map { it.valType }
        val _results = results.map { it.valType }

        store.addFunction(
            HostFunction(
                namespace, name,
                FunctionType.of(_params, _results)
            ) { instance, args ->
                implementation(instance.asWasmInstance(), args)
            }
        )
    }

    fun function(
        name: String,
        params: List<WasmType>,
        results: List<WasmType>,
        namespace: String = "env",
        implementation: (args: LongArray) -> LongArray?
    ) {
        function(
            namespace = namespace,
            name = name,
            params = params,
            results = results
        ) { instance, args ->
            implementation(args)
        }
    }

    fun function(
        name: String,
        params: List<WasmType>,
        namespace: String = "env",
        implementation: suspend (args: LongArray) -> Unit
    ) {
        function(
            namespace = namespace,
            name = name,
            params = params,
            results = listOf()
        ) { instance, args ->
            scope.launch { implementation(args) }
            null
        }
    }

    fun function(
        name: String,
        params: List<WasmType>,
        namespace: String = "env",
        implementation: suspend (WasmInstance, args: LongArray) -> Unit
    ) {
        function(
            namespace = namespace,
            name = name,
            params = params,
            results = listOf()
        ) { instance, args ->
            scope.launch { implementation(instance, args) }
            null
        }
    }

    fun function(
        name: String,
        results: List<WasmType>,
        namespace: String = "env",
        implementation: () -> LongArray
    ) {
        function(
            namespace = namespace,
            name = name,
            params = listOf(),
            results = results
        ) { instance, args ->
            implementation()
        }
    }

    fun function(
        name: String,
        results: List<WasmType>,
        namespace: String = "env",
        implementation: (WasmInstance) -> LongArray
    ) {
        function(
            namespace = namespace,
            name = name,
            params = listOf(),
            results = results
        ) { instance, args ->
            implementation(instance)
        }
    }

    fun function(
        name: String,
        namespace: String = "env",
        implementation: suspend () -> Unit
    ) {
        function(
            namespace = namespace,
            name = name,
            params = listOf(),
            results = listOf()
        ) { instance, args ->
            scope.launch { implementation() }
            null
        }
    }

    fun build(): WasmInstance {
        check(::_module.isInitialized) { "WASM module not initialized" }

        val instance = store.instantiate("klyx-wasm", _module.module)

        if (callInit) {
            runCatching {
                instance.export(initFunction).apply()
            }.onFailure {
                throw IllegalStateException("Failed to call init function: ${it.message}", it)
            }
        }

        return instance.asWasmInstance()
    }

    override fun close() {
        scope.cancel("WASM scope closed")
    }
}

@WasmDsl
class WasmModuleScope {
    fun file(file: File) = WasmModule(Parser.parse(file))
    fun bytes(bytes: ByteArray) = WasmModule(Parser.parse(bytes))
    fun inputStream(inputStream: InputStream) = WasmModule(Parser.parse(inputStream))
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun wasm(
    @BuilderInference
    block: WasmScope.() -> Unit
): WasmInstance {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return WasmScope().apply(block).build()
}
