package com.klyx.extension

import android.os.Environment
import com.klyx.core.extension.Extension
import com.klyx.core.theme.ThemeManager
import com.klyx.expect
import com.klyx.extension.modules.SystemModule
import com.klyx.wasm.ExperimentalWasm
import com.klyx.wasm.registerHostModule
import com.klyx.wasm.wasi.ExperimentalWasi
import com.klyx.wasm.wasi.directory
import com.klyx.wasm.wasi.env
import com.klyx.wasm.wasi.withWasi
import com.klyx.wasm.wasm
import kotlinx.io.asSource

object ExtensionLoader {
    val EXTERNAL_STORAGE: String = Environment.getExternalStorageDirectory().absolutePath

    @OptIn(ExperimentalWasm::class, ExperimentalWasi::class)
    suspend fun loadExtension(
        extension: Extension,
        shouldCallInit: Boolean = false
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

                callInit(
                    enabled = shouldCallInit,
                    function = "init-extension"
                )
            }
        }
    }
}
