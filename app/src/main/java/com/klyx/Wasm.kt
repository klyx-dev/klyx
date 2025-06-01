package com.klyx

import android.content.Context
import com.klyx.core.showShortToast
import kwasm.KWasmProgram
import kwasm.api.ByteBufferMemoryProvider
import kwasm.api.HostFunction
import kwasm.runtime.EmptyValue
import kwasm.runtime.IntValue

class Wasm(private val context: Context) {
    fun test(
        onColorChanged: (Int, Int, Int) -> Unit
    ) {
        val assets = context.assets
        val wasm = assets.open("wasm/my_extension.wasm")

        val tempMemory = ByteBufferMemoryProvider(2 * 1024 * 1024)
        val preProgram = KWasmProgram.builder(tempMemory)
            .withBinaryModule("ext", wasm)
            .withHostFunction(
                namespace = "env",
                name = "show_toast_impl",
                hostFunction = HostFunction { ptr: IntValue, length: IntValue, context ->
                    val bytes = ByteArray(length.value)
                    context.memory?.readBytes(bytes, ptr.value)
                    this.context.showShortToast(bytes.toString(Charsets.UTF_8))
                    EmptyValue
                }
            )
            .build()

        kotlin.runCatching {
            preProgram.getFunction("ext", "start")()
        }.onFailure {
            it.printStackTrace()
        }
    }
}
