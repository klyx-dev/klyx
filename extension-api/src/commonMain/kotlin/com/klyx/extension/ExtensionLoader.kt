package com.klyx.extension

import com.github.michaelbull.result.onFailure
import com.klyx.core.Environment
import com.klyx.core.extension.Extension
import com.klyx.core.file.source
import com.klyx.core.logging.logger
import com.klyx.core.theme.ThemeManager
import com.klyx.extension.internal.getenv
import com.klyx.extension.internal.rootDir
import com.klyx.extension.internal.userHomeDir
import com.klyx.extension.modules.GitHubModule
import com.klyx.extension.modules.HttpClientModule
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.snapshot

object ExtensionLoader {
    private val home = Environment.DeviceHomeDir

    @OptIn(ExperimentalWasmApi::class, ExperimentalWasiApi::class)
    suspend fun loadExtension(
        extension: Extension,
        shouldCallInit: Boolean = false,
        vararg extraHostModules: HostModule
    ): LocalExtension? {
        val logger = logger(extension.info.id)

        withContext(Dispatchers.IO) {
            extension.themeFiles.forEach { file ->
                ThemeManager.loadThemeFamily(file.source()).onFailure {
                    logger.error("Unable to load the theme: $it")
                }
            }
        }

        val wasmFile = extension.wasmFiles.firstOrNull() ?: return null
        val wasmBytes = withContext(Dispatchers.IO) { wasmFile.readBytes() }

        val stdout = Buffer()
        val stderr = Buffer()

        val instance = withContext(Dispatchers.Default) {
            wasm {
                module { bytes(wasmBytes) }

                withWasiPreview1 {
                    directory(home, home)

                    userHomeDir?.let {
                        directory(it, it)
                        env("USER_HOME", it)
                        env("HOME", it)
                        env { putAll(getenv()) }
                        workingDirectory(it)
                    }

                    directory(rootDir, "/")

                    stdout(StdioSinkProvider { stdout })
                    stderr(StdioSinkProvider { stderr })
                }

                registerHostModule(
                    RootModule(Root(logger)),
                    SystemModule,
                    ProcessModule,
                    HttpClientModule,
                    GitHubModule
                )
                registerHostModule(*extraHostModules)
            }
        }

        val localExtension = LocalExtension(extension, logger, instance)

        if (shouldCallInit) {
            withContext(Dispatchers.Default) {
                localExtension.initialize()
            }
        }

        withContext(Dispatchers.IO) {
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
        }
        return localExtension
    }
}
