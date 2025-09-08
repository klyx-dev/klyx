package com.klyx.extension

import com.github.michaelbull.result.onFailure
import com.klyx.core.Environment
import com.klyx.core.extension.Extension
import com.klyx.core.logging.logger
import com.klyx.core.theme.ThemeManager
import com.klyx.extension.modules.ProcessModule
import com.klyx.extension.modules.Root
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.snapshot

object ExtensionLoader {
    private val home = Environment.DeviceHomeDir

    @OptIn(
        DelicateCoroutinesApi::class,
        ExperimentalCoroutinesApi::class,
        ExperimentalWasmApi::class,
        ExperimentalWasiApi::class
    )
    suspend fun loadExtension(
        extension: Extension,
        shouldCallInit: Boolean = false,
        vararg extraHostModules: HostModule
    ) = run {
        val logger = logger(extension.info.id)
        val dispatcher = newFixedThreadPoolContext(10, extension.info.id)

        withContext(dispatcher) {
            extension.themeFiles.forEach { file ->
                ThemeManager.loadThemeFamily(file.source()).onFailure {
                    logger.error("Unable to load the theme: $it")
                }
            }

            extension.wasmFiles.firstOrNull()?.let { file ->
                val stdout = Buffer()
                val stderr = Buffer()

                val instance = wasm {
                    module { bytes(file.readBytes()) }

                    withWasiPreview1 {
                        directory(home, home)
                        workingDirectory(home)

                        stdout(StdioSinkProvider { stdout })
                        stderr(StdioSinkProvider { stderr })
                    }

                    registerHostModule(RootModule(Root(logger)), SystemModule, ProcessModule)
                    registerHostModule(*extraHostModules)
                }

                val localExtension = LocalExtension(extension, logger, instance, dispatcher)
                if (shouldCallInit) localExtension.initialize()

                stdout.snapshot()
                    .decodeToString()
                    .lineSequence()
                    .filter { it.isNotEmpty() }
                    .forEach(logger::info)

                stderr.snapshot()
                    .decodeToString()
                    .lineSequence()
                    .filter { it.isNotEmpty() }
                    .forEach(logger::error)

                localExtension
            }
        }
    }
}
