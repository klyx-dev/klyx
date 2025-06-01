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
        val wasm = assets.open("wasm/wasm_add.wasm")

        val memoryProvider = ByteBufferMemoryProvider(4096 * 4096)

        val program = KWasmProgram.builder(memoryProvider)
            .withBinaryModule("test", wasm)
            .withHostFunction(
                namespace = "env",
                name = "show_toast",
                hostFunction = HostFunction { offset: IntValue, length: IntValue, context ->
                    val bytes = ByteArray(length.value)
                    context.memory?.readBytes(bytes, offset.value)
                    val txt = bytes.toString(Charsets.UTF_8)
                    this.context.showShortToast(txt)
                    EmptyValue
                }
            )
            .withHostFunction(
                namespace = "env",
                name = "set_ui_color",
                hostFunction = HostFunction { r: IntValue, g: IntValue, b: IntValue, context ->
                    onColorChanged(r.value, g.value, b.value)
                    EmptyValue
                }
            )
            .build()

        val main = program.getFunction("test", "main")
        println("[main] signature: ${main.signature}")
        println("[main] argCount: ${main.argCount}")
        main()

        kotlin.runCatching {
            val add = program.getFunction("test", "add")
            println("[add] signature: ${add.signature}")
            println("[add] argCount: ${add.argCount}")
            println("1 + 2 using wasm = ${add(1, 2)}")

            val sub = program.getFunction("test", "sub")
            println("[sub] signature: ${sub.signature}")
            println("[sub] argCount: ${sub.argCount}")
            println("1 - 2 using wasm = ${sub(1, 2)}")

            val mul = program.getFunction("test", "mul")
            println("[mul] signature: ${mul.signature}")
            println("[mul] argCount: ${mul.argCount}")
            println("1 * 2 using wasm = ${mul(1, 2)}")

//            val div = program.getFunction("test", "div")
//            println("[div] signature: ${div.signature}")
//            println("[div] argCount: ${div.argCount}")
//            println("1 / 2 using wasm = ${div(1, 2)}")
        }.onFailure {
            it.printStackTrace()
        }
    }
}
