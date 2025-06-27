package com.klyx.extension

import android.content.Context
import com.klyx.core.extension.Extension
import com.klyx.core.file.rawFile
import com.klyx.core.theme.ThemeManager
import com.klyx.extension.impl.Android
import com.klyx.extension.impl.FileSystem
import com.klyx.extension.impl.Logger
import kotlinx.io.asSource
import kwasm.KWasmProgram
import kwasm.api.ByteBufferMemoryProvider

class ExtensionFactory(private vararg val modules: ExtensionHostModule) {
    suspend fun loadExtension(extension: Extension, callStartFunction: Boolean = false): KWasmProgram? {
        val id = extension.toml.id

        extension.themeFiles.forEach { file ->
            if (file.inputStream() == null) return@forEach

            ThemeManager.loadThemeFamily(file.inputStream()!!.asSource()).getOrThrow()
        }

        return extension.wasmFiles.firstOrNull()?.let { wasm ->
            val memorySize = extension.toml.requestedMemorySize ?: 1 // Default to 1MB if not specified
            val builder = KWasmProgram
                .builder(ByteBufferMemoryProvider(memorySize * 1024L * 1024L))
                .withBinaryModule(id, wasm.rawFile())

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

            program
        }
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
