package com.klyx.extension

import com.klyx.core.extension.Extension
import com.klyx.core.file.source
import com.klyx.core.io.Paths
import com.klyx.core.io.homeDir
import com.klyx.core.io.root
import com.klyx.core.language.LanguageName
import com.klyx.core.language.LanguageRegistry
import com.klyx.core.logging.KxLogger
import com.klyx.core.logging.logger
import com.klyx.core.theme.ThemeManager
import com.klyx.extension.internal.getenv
import com.klyx.extension.internal.userHomeDir
import com.klyx.extension.modules.GitHubModule
import com.klyx.extension.modules.HttpClientModule
import com.klyx.extension.modules.NodeJsModule
import com.klyx.extension.modules.PlatformModule
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
import io.itsvks.anyhow.onFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.readString
import kotlin.jvm.JvmSynthetic

object ExtensionLoader {
    private val home = homeDir().toString()

    @OptIn(ExperimentalWasmApi::class, ExperimentalWasiApi::class)
    suspend fun loadExtension(
        extension: Extension,
        shouldCallInit: Boolean = false,
        vararg extraHostModules: HostModule
    ): WasmExtension? {
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

        val instance = withContext(Dispatchers.Default) {
            val systemEnvs = getenv()

            wasm {
                module { bytes(wasmBytes) }

                withWasiPreview1 {
                    directory(home, home)

                    userHomeDir?.let {
                        directory(it, it)
                        env("USER_HOME", it)
                        env("HOME", it)
                        env { putAll(systemEnvs) }
                    }

                    directory(Paths.dataDir.toString(), Paths.dataDir.toString())
                    directory(Paths.root.toString(), "/")
                    workingDirectory(Paths.root.toString())

                    stdout(createExtensionStdoutSink(logger))
                    stderr(createExtensionStderrSink(logger))
                }

                registerHostModule(
                    RootModule(Root(logger)),
                    SystemModule,
                    ProcessModule,
                    HttpClientModule,
                    GitHubModule,
                    PlatformModule,
                    NodeJsModule
                )
                registerHostModule(*extraHostModules)
            }
        }

        val wasmExtension = WasmExtension(extension, logger, instance)

        val languages = LanguageRegistry.INSTANCE
        for ((languageServerId, languageServerConfig) in extension.info.languageServers) {
            for (language in languageServerConfig.languages) {
                languages.registerLspAdapter(
                    LanguageName(language),
                    ExtensionLspAdapter(
                        wasmExtension,
                        languageServerId,
                        LanguageName(language)
                    )
                )
            }
        }

        if (shouldCallInit) {
            withContext(Dispatchers.Default) {
                wasmExtension.initialize()
            }
        }

        return wasmExtension
    }
}

@JvmSynthetic
private fun createExtensionStdoutSink(logger: KxLogger): StdioSinkProvider {
    return StdioSinkProvider {
        object : RawSink {
            override fun write(source: Buffer, byteCount: Long) {
                val text = source.readString(byteCount)
                logger.info { text }
            }

            override fun flush() {}
            override fun close() {}
        }
    }
}

@JvmSynthetic
private fun createExtensionStderrSink(logger: KxLogger): StdioSinkProvider {
    return StdioSinkProvider {
        object : RawSink {
            override fun write(source: Buffer, byteCount: Long) {
                val text = source.readString(byteCount)
                logger.error { text }
            }

            override fun flush() {}
            override fun close() {}
        }
    }
}
