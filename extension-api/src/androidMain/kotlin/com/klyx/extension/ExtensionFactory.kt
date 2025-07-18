package com.klyx.extension

import android.content.Context
import com.klyx.core.extension.Extension
import com.klyx.core.file.rawFile
import com.klyx.core.theme.ThemeManager
import com.klyx.expect
import com.klyx.extension.impl.Android
import com.klyx.extension.impl.FileSystem
import com.klyx.extension.impl.Logger
import kotlinx.io.asSource
import kwasm.KWasmProgram
import kwasm.api.ByteBufferMemoryProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ExtensionFactory(
    modules: List<ExtensionHostModule> = emptyList()
) : KoinComponent {
    private val context: Context by inject()
    private val _modules = mutableListOf<ExtensionHostModule>()

    init {
        _modules.addAll(
            arrayOf(
                Android(context),
                FileSystem(),
                Logger(),
                *modules.toTypedArray()
            )
        )
    }

    suspend fun loadExtension(
        extension: Extension,
        callInit: Boolean = false
    ): KWasmProgram? {
        val id = extension.toml.id

        extension.themeFiles.forEach { file ->
            if (file.inputStream() == null) return@forEach

            ThemeManager
                .loadThemeFamily(file.inputStream()!!.asSource())
                .expect("Unable to load the theme")
        }

        return extension.wasmFiles.firstOrNull()?.let { wasm ->
            val memorySize =
                extension.toml.requestedMemorySize ?: 1 // Default to 1MB if not specified
            val builder = KWasmProgram
                .builder(ByteBufferMemoryProvider(memorySize * 1024L * 1024L))
                .withBinaryModule(id, wasm.rawFile())

            for (module in _modules) {
                for (func in module.getHostFunctions()) {
                    builder.withHostFunction(
                        namespace = module.namespace,
                        name = func.name + "_impl",
                        hostFunction = func.function
                    )
                }
            }

            val program = builder.build()

            if (callInit) {
                runCatching {
                    program.getFunction(id, "init")()
                }.onFailure {
                    throw ExtensionLoadException("Failed to call init function: ${it.message}", it)
                }
            }

            program
        }
    }

    companion object {
        private var factory: ExtensionFactory? = null

        @JvmStatic
        fun getInstance() = run {
            if (factory == null) {
                factory = ExtensionFactory()
            }
            factory!!
        }
    }
}
