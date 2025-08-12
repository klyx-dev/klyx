package com.klyx.wasm

import com.dylibso.chicory.runtime.ExportFunction
import com.klyx.borrow.ref

@ExperimentalWasmApi
class WasmHostCallable internal constructor(
    private val instance: WasmInstance,
    private val function: ExportFunction
) {
    private val memory by lazy { instance.memory }

    operator fun invoke(vararg args: Any?): LongArray {
        val result = function.call(*buildArgs(args))
        return result ?: longArrayOf()
    }

    private fun buildArgs(args: Array<out Any?>): LongArray {
        var totalSize = 0
        for (arg in args) {
            totalSize += when (arg) {
                is String, is ByteArray -> 2
                else -> 1
            }
        }

        val result = LongArray(totalSize)
        var i = 0
        for (arg in args) {
            when (arg) {
                is Long -> result[i++] = arg
                is Int -> result[i++] = arg.toLong()
                is Short -> result[i++] = arg.toLong()
                is Byte -> result[i++] = arg.toLong()
                is Float -> result[i++] = arg.toBits().toLong()
                is Double -> result[i++] = arg.toBits()
                is String -> {
                    val (ptr, len) = memory.writeString(arg)
                    result[i++] = ptr.toLong()
                    result[i++] = len.toLong()
                }

                is ByteArray -> {
                    val (ptr, len) = memory.write(arg)
                    result[i++] = ptr.toLong()
                    result[i++] = len.toLong()
                }

                else -> {
                    result[i++] = if (arg == null) 0L else ref(arg).rawPointer
                }
            }
        }

        return result
    }
}

@OptIn(ExperimentalWasmApi::class)
internal fun ExportFunction.toWasmHostCallable(instance: WasmInstance): WasmHostCallable {
    return WasmHostCallable(instance, this)
}

private fun ExportFunction.call(vararg args: Long) = apply(*args)
