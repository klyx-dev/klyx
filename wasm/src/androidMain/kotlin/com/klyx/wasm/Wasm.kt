package com.klyx.wasm

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.wasm.Parser
import com.dylibso.chicory.wasm.types.FunctionType
import com.dylibso.chicory.wasm.types.ValType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

@ExperimentalWasm
enum class WasmType(internal vararg val valType: ValType) {
    I32(ValType.I32),
    I64(ValType.I64),
    F32(ValType.F32),
    F64(ValType.F64),
    String(ValType.I32, ValType.I32)
}

val Long.i32 get() = this.toInt()
val Long.i64 get() = this
val Long.f32 get() = this.toFloat()
val Long.f64 get() = this.toDouble()

@DslMarker
internal annotation class WasmDsl

@WasmDsl
@ExperimentalWasm
class WasmScope @PublishedApi internal constructor() : AutoCloseable {
    val store = WasmStore()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var callInit = false
    private var initFunction = "init-extension"

    private lateinit var _module: WasmModule

    @OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
    fun module(
        @BuilderInference
        block: WasmModuleScope.() -> WasmModule
    ) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        _module = block(WasmModuleScope())
    }

    fun callInit(
        enabled: Boolean = true,
        function: String = "init-extension"
    ) = apply {
        this.callInit = enabled
        this.initFunction = function
    }

    fun function(
        name: String,
        params: List<WasmType>,
        results: List<WasmType>,
        moduleName: String = "env",
        implementation: FunctionScope.(args: LongArray) -> LongArray?
    ) = apply {
        val _params = params.flatMap { it.valType.toList() }
        val _results = results.flatMap { it.valType.toList() }

        store.addFunction(
            HostFunction(
                moduleName, name,
                FunctionType.of(_params, _results)
            ) { instance, args ->
                FunctionScope(instance.asWasmInstance()).run { implementation(args) }
            }
        )
    }

    fun function(
        name: String,
        params: List<WasmType>,
        moduleName: String = "env",
        implementation: suspend FunctionScope.(args: LongArray) -> Unit
    ) = apply {
        function(
            moduleName = moduleName,
            name = name,
            params = params,
            results = listOf()
        ) { args ->
            scope.launch { implementation(args) }
            null
        }
    }

    fun function(
        name: String,
        results: List<WasmType>,
        moduleName: String = "env",
        implementation: FunctionScope.() -> LongArray
    ) = apply {
        function(
            moduleName = moduleName,
            name = name,
            params = listOf(),
            results = results
        ) { _ -> implementation() }
    }

    fun function(
        name: String,
        moduleName: String = "env",
        implementation: suspend FunctionScope.() -> Unit
    ) = apply {
        function(
            moduleName = moduleName,
            name = name,
            params = listOf(),
            results = listOf()
        ) { _ ->
            scope.launch { implementation() }
            null
        }
    }

    @PublishedApi
    internal fun build(): WasmInstance {
        check(::_module.isInitialized) { "WASM module not initialized. Did you forget to call `module { ... }`?" }
        val instance = store.instantiate("klyx-wasm", _module)

        if (callInit) {
            runCatching {
                val init = instance.function(initFunction)
                init()
            }.onFailure {
                it.printStackTrace()
                throw it
            }
        }

        return instance
    }

    override fun close() {
        scope.cancel("WASM scope closed")
    }
}

@WasmDsl
@ExperimentalWasm
class WasmModuleScope {
    fun file(file: File) = WasmModule(Parser.parse(file))
    fun bytes(bytes: ByteArray) = WasmModule(Parser.parse(bytes))
    fun inputStream(inputStream: InputStream) = WasmModule(Parser.parse(inputStream))
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
@ExperimentalWasm
inline fun wasm(
    @BuilderInference
    block: WasmScope.() -> Unit
): WasmInstance {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return WasmScope().apply(block).build()
}
