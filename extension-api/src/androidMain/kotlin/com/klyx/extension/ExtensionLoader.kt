package com.klyx.extension

import android.os.Environment
import com.klyx.core.extension.Extension
import com.klyx.core.theme.ThemeManager
import com.klyx.expect
import com.klyx.extension.modules.ProcessModule
import com.klyx.extension.modules.RootModule
import com.klyx.extension.modules.SystemModule
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.HostModule
import com.klyx.wasm.registerHostModule
import com.klyx.wasm.wasi.ExperimentalWasiApi
import com.klyx.wasm.wasi.StdioSinkProvider
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
                    directory(EXTERNAL_STORAGE, EXTERNAL_STORAGE)
                    workingDirectory(EXTERNAL_STORAGE)

                    stdout(StdioSinkProvider { System.out })
                    stderr(StdioSinkProvider { System.err })
                }

                registerHostModule(RootModule, SystemModule, ProcessModule)
                registerHostModule(*hostModule)

                callInit(
                    enabled = shouldCallInit,
                    functionName = "init-extension"
                )
            }
        }
    }
}
