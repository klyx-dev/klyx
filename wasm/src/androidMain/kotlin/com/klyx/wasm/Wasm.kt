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

@ExperimentalWasmApi
sealed interface WasmType {
    val valTypes: List<ValType>

    data object I32 : WasmType {
        override val valTypes = listOf(ValType.I32)
    }

    data object I64 : WasmType {
        override val valTypes = listOf(ValType.I64)
    }

    data object F32 : WasmType {
        override val valTypes = listOf(ValType.F32)
    }

    data object F64 : WasmType {
        override val valTypes = listOf(ValType.F64)
    }

    data object String : WasmType {
        override val valTypes = listOf(ValType.I32, ValType.I32)
    }
}

val Long.i32 get() = toInt()
val Long.i64 get() = this
val Long.f32 get() = toFloat()
val Long.f64 get() = toDouble()

typealias FunctionName = String
typealias ModuleName = String

@DslMarker
internal annotation class WasmDsl

@WasmDsl
@ExperimentalWasmApi
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
        functionName: FunctionName = "init-extension"
    ) = apply {
        this.callInit = enabled
        this.initFunction = functionName
    }

    fun function(
        name: FunctionName,
        params: List<WasmType>,
        results: List<WasmType>,
        moduleName: ModuleName = "env",
        implementation: FunctionScope.(args: LongArray) -> LongArray?
    ) = apply {
        val _params = params.flatMap { it.valTypes }
        val _results = results.flatMap { it.valTypes }

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
        name: FunctionName,
        params: List<WasmType>,
        moduleName: ModuleName = "env",
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
        name: FunctionName,
        results: List<WasmType>,
        moduleName: ModuleName = "env",
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
        name: FunctionName,
        moduleName: ModuleName = "env",
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
@ExperimentalWasmApi
class WasmModuleScope {
    fun file(file: File) = WasmModule(Parser.parse(file))
    fun bytes(bytes: ByteArray) = WasmModule(Parser.parse(bytes))
    fun inputStream(inputStream: InputStream) = WasmModule(Parser.parse(inputStream))
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
@ExperimentalWasmApi
inline fun wasm(
    @BuilderInference
    block: WasmScope.() -> Unit
): WasmInstance {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return WasmScope().apply(block).build()
}
