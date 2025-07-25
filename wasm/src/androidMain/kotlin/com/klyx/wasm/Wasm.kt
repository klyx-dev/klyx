package com.klyx.wasm

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Store
import com.dylibso.chicory.wasm.Parser
import com.dylibso.chicory.wasm.WasmModule
import com.dylibso.chicory.wasm.types.FunctionType
import com.dylibso.chicory.wasm.types.ValType
import kotlinx.coroutines.MainScope
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
class WasmBuilder {
    private val scope = MainScope()
    private val store = Store()
    private var callInit = false

    private lateinit var _module: WasmModule

    @OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
    fun module(
        @BuilderInference
        block: WasmModuleBuilder.() -> Unit
    ) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        _module = WasmModuleBuilder().apply(block).build()
    }

    fun callInit(enabled: Boolean = true) {
        this.callInit = enabled
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

        val instance = store.instantiate("klyx-wasm", _module)

        if (callInit) {
            runCatching {
                instance.export("init").apply()
            }.onFailure {
                throw IllegalStateException("Failed to call init function: ${it.message}", it)
            }
        }

        return instance.asWasmInstance()
    }
}

@WasmDsl
class WasmModuleBuilder {
    private var file: File? = null
    private var bytes: ByteArray? = null
    private var inputStream: InputStream? = null

    fun file(file: File) {
        this.file = file
    }

    fun bytes(bytes: ByteArray) {
        this.bytes = bytes
    }

    fun inputStream(inputStream: InputStream) {
        this.inputStream = inputStream
    }

    fun build(): WasmModule {
        val source = listOf(file, bytes, inputStream).singleOrNull { it != null }
        require(source != null) { "WASM module source must be provided exactly once using file(), bytes(), or inputStream()" }

        return when (source) {
            is File -> Parser.parse(source)
            is ByteArray -> Parser.parse(source)
            is InputStream -> Parser.parse(source)
            else -> throw IllegalArgumentException("No source provided")
        }
    }
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun wasm(
    @BuilderInference
    block: WasmBuilder.() -> Unit
): WasmInstance {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return WasmBuilder().apply(block).build()
}
