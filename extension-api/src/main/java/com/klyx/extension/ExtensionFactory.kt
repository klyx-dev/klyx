package com.klyx.extension

import android.content.Context
import com.klyx.extension.impl.Android
import com.klyx.extension.impl.FileSystem
import com.klyx.extension.impl.Logger
import kwasm.KWasmProgram
import kwasm.api.ByteBufferMemoryProvider

class ExtensionFactory(private vararg val modules: ExtensionHostModule) {
    fun loadExtension(extension: Extension, callStartFunction: Boolean = false): KWasmProgram {
        val (input, toml) = extension
        val id = toml.id

        val builder = KWasmProgram.builder(ByteBufferMemoryProvider(toml.requestedMemorySize * 1024L * 1024L))
            .withBinaryModule(id, input)

        for (module in modules) {
            for (func in module.getHostFunctions()) {
                builder.withHostFunction(
                    namespace = module.namespace,
                    name = func.name + "_impl",
                    hostFunction = func.function
                )
            }
        }

        val program = builder.build()

        if (callStartFunction) {
            runCatching {
                program.getFunction(id, "start")()
            }.onFailure {
                throw ExtensionLoadException("Failed to call start function: ${it.message}", it)
            }
        }

        return program
    }

    companion object {
        @JvmStatic
        fun create(context: Context) = ExtensionFactory(
            Android(context),
            FileSystem(),
            Logger()
        )
    }
}
