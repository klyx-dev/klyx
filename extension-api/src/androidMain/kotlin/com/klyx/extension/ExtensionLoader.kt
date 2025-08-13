package com.klyx.extension

import android.os.Environment
import com.klyx.core.extension.Extension
import com.klyx.core.theme.ThemeManager
import com.klyx.expect
import com.klyx.extension.api.Result
import com.klyx.extension.api.SystemWorktree
import com.klyx.extension.api.readCommandResult
import com.klyx.extension.modules.RootModule
import com.klyx.extension.modules.SystemModule
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.HostModule
import com.klyx.wasm.registerHostModule
import com.klyx.wasm.type.collections.wasmListOf
import com.klyx.wasm.wasi.ExperimentalWasiApi
import com.klyx.wasm.wasi.directory
import com.klyx.wasm.wasi.env
import com.klyx.wasm.wasi.withWasi
import com.klyx.wasm.wasm
import kotlinx.io.asSource

object ExtensionLoader {
    val EXTERNAL_STORAGE: String = Environment.getExternalStorageDirectory().absolutePath

    @OptIn(ExperimentalWasmApi::class, ExperimentalWasiApi::class)
    suspend fun loadExtension(
        extension: Extension,
        shouldCallInit: Boolean = false,
        vararg hostModule: HostModule
    ) {
        extension.themeFiles.forEach { file ->
            if (file.inputStream() == null) return@forEach

            ThemeManager
                .loadThemeFamily(file.inputStream()!!.asSource())
                .expect("Unable to load the theme")
        }

        extension.wasmFiles.firstOrNull()?.let { wasm ->
            wasm {
                module { bytes(wasm.readBytes()) }

                withWasi {
                    inheritSystem()
                    env("PWD", EXTERNAL_STORAGE)
                    directory(EXTERNAL_STORAGE, EXTERNAL_STORAGE)
                }

                registerHostModule(SystemModule())
                registerHostModule(RootModule())
                registerHostModule(*hostModule)

                callInit(
                    enabled = shouldCallInit,
                    functionName = "init-extension"
                )
            }.also { instance ->
                val memory = instance.memory

                val func = instance.function("language-server-command")
                val ptr = func("JSON", SystemWorktree)

                val result = memory.readCommandResult(ptr)

                with(memory) {
                    println(wasmListOf("hello", "world").toString(this))
                }

                when (result) {
                    is Result.Ok -> {
                        with(memory) {
                            println("Ok: ${result.value.toString(memory)}")
                            println(result.value.args.isEmpty())
                        }
                    }

                    is Result.Err -> {
                        println("Error: ${result.error}")
                    }
                }
            }
        }
    }
}
