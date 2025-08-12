package com.klyx.wasm

import com.dylibso.chicory.runtime.Instance
import com.klyx.wasm.utils.i32

@OptIn(ExperimentalWasmApi::class)
internal fun Instance.asWasmInstance() = WasmInstance(this)

@ExperimentalWasmApi
class WasmInstance internal constructor(
    private val instance: Instance
) {
    internal val realloc
        get() = try {
            function("cabi_realloc")
        } catch (_: Exception) {
            error("cabi_realloc not exported by WASM module")
        }

    val memory by lazy { WasmMemory(this, instance.memory()) }

    fun function(name: String) = instance.export(name).toWasmHostCallable(this)
    fun call(name: String, vararg args: Any?) = function(name)(*args)
}

/**
 * Allocate memory in WASM.
 * @param size number of bytes to allocate
 * @param align alignment (usually 1, 4, or 8)
 * @return pointer to allocated memory
 */
@OptIn(ExperimentalWasmApi::class)
fun WasmInstance.alloc(size: Int, align: Int = 1): Int {
    val ptr = realloc(0L, 0L, align.toLong(), size.toLong()).raw.i32
    return ptr
}

/**
 * Free memory in WASM.
 * @param ptr pointer to the memory to free
 * @param oldSize size of the memory block being freed
 * @param align alignment used for the allocation
 */
@OptIn(ExperimentalWasmApi::class)
fun WasmInstance.free(ptr: Int, oldSize: Int, align: Int = 1) {
    realloc(ptr.toLong(), oldSize.toLong(), align.toLong(), 0L)
}

/**
 * Reallocate memory in WASM.
 * @param ptr pointer to the existing memory block
 * @param oldSize size of the old block
 * @param newSize desired size of the new block
 * @param align alignment
 * @return new pointer (may be same or different)
 */
@OptIn(ExperimentalWasmApi::class)
fun WasmInstance.realloc(ptr: Int, oldSize: Int, newSize: Int, align: Int = 1): Int {
    return realloc(ptr.toLong(), oldSize.toLong(), align.toLong(), newSize.toLong()).raw.i32
}
