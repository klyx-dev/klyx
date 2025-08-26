@file:Suppress("EmptyInitBlock")

package com.klyx.wasm

import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.mapError
import com.klyx.wasm.internal.InternalExperimentalWasmApi
import com.klyx.wasm.internal.asExecutionValue
import com.klyx.wasm.internal.asInt
import io.github.charlietap.chasm.embedding.error.ChasmError
import io.github.charlietap.chasm.embedding.invoke
import io.github.charlietap.chasm.embedding.shapes.ChasmResult
import io.github.charlietap.chasm.embedding.shapes.Instance
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.embedding.shapes.onError

@ExperimentalWasmApi
class WasmInstance internal constructor(
    private val instance: Instance,
    private val store: Store
) {
    val memory by lazy {
        val export = instance.exports.find { it.name == "memory" }
        requireNotNull(export) { "Export 'memory' not found" }
        export.asMemory(store, this)
    }

    fun call(functionName: String, args: List<WasmValue> = emptyList()) = run {
        invoke(store, instance, functionName, args.asExecutionValues()).onError {
            println("Error calling $functionName: ${it.error}")
        }.asResult().getOrThrow(::WasmRuntimeException).toWasmValues()
    }

    fun function(name: String) = WasmFunction(name, this)

    /**
     * Reallocate memory in WASM.
     * @param pointer pointer to the existing memory block
     * @param oldSize size of the old block
     * @param newSize desired size of the new block
     * @param align alignment
     * @return new pointer (may be same or different)
     */
    @OptIn(InternalExperimentalWasmApi::class)
    internal fun realloc(pointer: Int, oldSize: Int, newSize: Int, align: Int = 1) = run {
        invoke(
            store = store,
            instance = instance,
            name = "cabi_realloc",
            args = listOf(
                pointer.asExecutionValue(),
                oldSize.asExecutionValue(),
                align.asExecutionValue(),
                newSize.asExecutionValue()
            )
        ).asResult().mapError(::WasmRuntimeException).getOrThrow().first().asInt()
    }
}

/**
 * Allocate memory in WASM.
 * @param size number of bytes to allocate
 * @param align alignment (usually 1, 4, or 8)
 * @return pointer to allocated memory
 */
@OptIn(ExperimentalWasmApi::class)
fun WasmInstance.alloc(size: Int, align: Int = 1) = realloc(0, 0, size, align)

/**
 * Free memory in WASM.
 * @param pointer pointer to the memory to free
 * @param oldSize size of the memory block being freed
 * @param align alignment used for the allocation
 */
@OptIn(ExperimentalWasmApi::class)
fun WasmInstance.free(pointer: Int, oldSize: Int, align: Int = 1) {
    realloc(pointer, oldSize, 0, align)
}

@OptIn(ExperimentalWasmApi::class)
internal fun Instance.asWasmInstance(store: Store) = WasmInstance(this, store)

@OptIn(ExperimentalWasmApi::class)
internal fun ChasmResult<Instance, ChasmError.ExecutionError>.asWasmInstance(store: Store) = run {
    asResult().getOrThrow { err ->
        val imports = parseImports(err.message)
        imports.forEach { println(it) }
        err
    }.asWasmInstance(store)
}
