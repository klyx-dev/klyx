package com.klyx.wasm

import com.klyx.borrow.ref
import com.klyx.wasm.internal.InternalExperimentalWasmApi
import com.klyx.wasm.internal.asExecutionValue
import com.klyx.wasm.internal.toUtf8ByteArray
import io.github.charlietap.chasm.runtime.value.ExecutionValue

@ExperimentalWasmApi
class WasmFunction(
    private val name: String,
    private val instance: WasmInstance,
) {
    private val memory get() = instance.memory

    operator fun invoke(vararg args: Any?) = instance.call(name, buildArgs(args).toWasmValues())

    @OptIn(InternalExperimentalWasmApi::class)
    private fun buildArgs(args: Array<out Any?>): List<ExecutionValue> {
        val result = mutableListOf<ExecutionValue>()

        for (arg in args) {
            when (arg) {
                is Long -> result += arg.asExecutionValue()
                is Int -> result += arg.asExecutionValue()
                is UInt -> result += arg.asExecutionValue()
                is ULong -> result += arg.asExecutionValue()
                is Short -> result += arg.asExecutionValue()
                is Byte -> result += arg.asExecutionValue()
                is Float -> result += arg.asExecutionValue()
                is Double -> result += arg.asExecutionValue()
                is String -> {
                    val (ptr, len) = memory.allocateAndWrite(arg.toUtf8ByteArray())
                    result += ptr.asExecutionValue()
                    result += len.asExecutionValue()
                }

                is ByteArray -> {
                    val (ptr, len) = memory.allocateAndWrite(arg)
                    result += ptr.asExecutionValue()
                    result += len.asExecutionValue()
                }

                else -> {
                    result += if (arg == null) 0L.asExecutionValue() else ref(arg).rawPointer.asExecutionValue()
                }
            }
        }

        return result
    }
}
