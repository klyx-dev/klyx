package com.klyx.extension

import android.os.Environment
import com.klyx.core.extension.Extension
import com.klyx.core.logging.logger
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
import com.klyx.wasm.wasi.withWasiPreview1
import com.klyx.wasm.wasm
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import kotlinx.io.asSource
import java.io.ByteArrayOutputStream

object ExtensionLoader {
    val EXTERNAL_STORAGE: String = Environment.getExternalStorageDirectory().absolutePath

    @OptIn(ExperimentalWasmApi::class, ExperimentalWasiApi::class, DelicateCoroutinesApi::class)
    suspend fun loadExtension(
        extension: Extension,
        shouldCallInit: Boolean = false,
        vararg hostModule: HostModule
    ) = withContext(newFixedThreadPoolContext(10, extension.toml.id)) {
        val logger = logger(extension.toml.name)

        extension.themeFiles.forEach { file ->
            if (file.inputStream() == null) return@forEach

            ThemeManager
                .loadThemeFamily(file.inputStream()!!.asSource())
                .expect("Unable to load the theme")
        }

        extension.wasmFiles.firstOrNull()?.let { wasm ->
            val stdoutBuffer = ByteArrayOutputStream()
            val stderrBuffer = ByteArrayOutputStream()

            val instance = wasm {
                module { bytes(wasm.readBytes()) }

                withWasiPreview1 {
                    directory(EXTERNAL_STORAGE, EXTERNAL_STORAGE)
                    workingDirectory(EXTERNAL_STORAGE)

                    stdout(StdioSinkProvider { stdoutBuffer })
                    stderr(StdioSinkProvider { stderrBuffer })
                }

                registerHostModule(RootModule, SystemModule, ProcessModule)
                registerHostModule(*hostModule)
            }

            if (shouldCallInit) {
                instance.call("init-extension")
            }

            stdoutBuffer.use { stdout ->
                logger.info("${stdout.toString("UTF-8")}")
            }

            stderrBuffer.use { stderr ->
                logger.error("${stderr.toString("UTF-8")}")
            }
        }
    }
}
