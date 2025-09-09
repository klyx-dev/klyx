@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.modules

import arrow.core.getOrElse
import arrow.core.toOption
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.klyx.core.Notifier
import com.klyx.core.file.humanBytes
import com.klyx.core.logging.KxLogger
import com.klyx.core.settings.LspSettings
import com.klyx.core.settings.SettingsManager
import com.klyx.extension.api.Worktree
import com.klyx.extension.api.lsp.CommandSettings
import com.klyx.extension.api.lsp.LanguageServerInstallationStatus
import com.klyx.extension.api.lsp.parseLanguageServerInstallationStatus
import com.klyx.extension.api.parseSettingsLocation
import com.klyx.extension.internal.asOption
import com.klyx.extension.internal.toWasmOption
import com.klyx.extension.internal.toWasmResult
import com.klyx.pointer.asPointer
import com.klyx.pointer.dropPtr
import com.klyx.pointer.value
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.annotations.HostFunction
import com.klyx.wasm.annotations.HostModule
import com.klyx.wasm.type.WasmUnit
import com.klyx.wasm.type.collections.toWasmList
import com.klyx.wasm.type.toBuffer
import com.klyx.wasm.type.toWasm
import com.klyx.wasm.type.wstr
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.klyx.wasm.type.Err as WasmErr
import com.klyx.wasm.type.Ok as WasmOk

@HostModule("\$root")
class Root(
    private val logger: KxLogger
) : KoinComponent {
    private val notifier: Notifier by inject()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        namingStrategy = JsonNamingStrategy.SnakeCase
        isLenient = true
    }

    @HostFunction("[resource-drop]worktree")
    fun dropWorktree(worktreePtr: Int) {
        val ptr = worktreePtr.asPointer()

        if (!dropPtr(ptr)) {
            logger.warn("Failed to drop worktree (pointer: $ptr)")
        }
    }

    @HostFunction("[method]worktree.read-text-file")
    fun WasmMemory.worktreeReadTextFile(ptr: Int, path: String, retPtr: Int) {
        val worktree = ptr.asPointer().value<Worktree>()
        val result = worktree.readTextFile(path).toWasmResult()
        write(retPtr, result.toBuffer())
    }

    @HostFunction("[method]worktree.id")
    fun worktreeId(ptr: Int) = ptr.asPointer().value<Worktree>().id

    @HostFunction("[method]worktree.root-path")
    fun WasmMemory.worktreeRootPath(ptr: Int, retPtr: Int) {
        val worktree = ptr.asPointer().value<Worktree>()
        val rootPath = worktree.rootFile.absolutePath.toWasm()
        write(retPtr, rootPath.toBuffer())
    }

    @HostFunction("[method]worktree.which")
    fun WasmMemory.worktreeWhich(ptr: Int, binaryName: String, retPtr: Int) {
        val worktree = ptr.asPointer().value<Worktree>()
        val result = worktree.which(binaryName).toWasmOption()
        write(retPtr, result.toBuffer())
    }

    @HostFunction("[method]worktree.shell-env")
    fun WasmMemory.worktreeShellEnv(ptr: Int, retPtr: Int) {
        val worktree = ptr.asPointer().value<Worktree>()
        val envVars = worktree.shellEnv().toWasmList()
        write(retPtr, envVars.toBuffer())
    }

    @HostFunction
    fun WasmMemory.makeFileExecutable(path: String, resultPtr: Int) {
        val res = com.klyx.extension.internal.makeFileExecutable(path).toWasmResult()
        write(resultPtr, res.toBuffer())
    }

    @HostFunction
    fun WasmMemory.downloadFile(url: String, path: String, resultPtr: Int) = runBlocking {
        val result = try {
            Path(path).parent?.let { SystemFileSystem.createDirectories(it) }
            com.klyx.core.file.downloadFile(
                url = url,
                outputPath = path,
                onDownload = { bytesSentTotal, contentLength ->
                    val progress = if (contentLength == null) 0f else bytesSentTotal.toFloat() / contentLength

                    logger.progress((progress * 100).toInt()) {
                        "(${bytesSentTotal.humanBytes()} / ${contentLength?.humanBytes()}) Downloading $url"
                    }
                },
                onComplete = {
                    logger.info { "" }
                }
            )
            WasmOk(WasmUnit)
        } catch (e: Exception) {
            WasmErr("$e".wstr)
        }
        write(resultPtr, result.toBuffer())
    }

    @HostFunction
    fun setLanguageServerInstallationStatus(languageServerName: String, tag: Int, failedReason: String) {
        val status = parseLanguageServerInstallationStatus(tag, failedReason)
        when (status) {
            LanguageServerInstallationStatus.CheckingForUpdate -> {
                logger.progress { "[$languageServerName] checking for updates" }
            }

            LanguageServerInstallationStatus.Downloading -> {
                logger.progress { "[$languageServerName] downloading" }
            }

            is LanguageServerInstallationStatus.Failed -> {
                logger.error { "[$languageServerName] failed: ${status.reason}" }
            }

            LanguageServerInstallationStatus.None -> {
                logger.info { "" }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @HostFunction
    fun WasmMemory.getSettings(
        op1: Int,
        worktreeId: Long,
        path: String,
        category: String,
        op2: Int,
        key: String,
        retPtr: Int
    ) {
        val allSettings = SettingsManager.settings.value
        val defaultSettings = SettingsManager.defaultSettings

        @Suppress("UnusedVariable")
        val location = parseSettingsLocation(op1, worktreeId, path)
        val key = key.asOption(op2)

        val result = when (category) {
            "language" -> {
                val languageSettings = key.map {
                    allSettings.languages[it] //?: error("Language setting not found: $it")
                }

                val s = languageSettings.map { settings ->
                    buildJsonObject {
                        if (settings != null) {
                            settings["tab_size"]?.let { put("tab_size", it) }
                        }
                    }.toJson()
                }

                s.fold({ Err("Language setting not found: $key") }, ::Ok)
            }

            "lsp" -> {
                val lspSettings = key.flatMap {
                    (allSettings.lsp[it] ?: defaultSettings.lsp[it]).toOption()
                }.getOrElse { LspSettings() }

                Ok(lspSettings.let { s ->
                    com.klyx.extension.api.lsp.LspSettings(
                        binary = s.binary?.let {
                            CommandSettings(
                                path = it.path,
                                arguments = it.arguments,
                                env = it.env
                            )
                        },
                        initializationOptions = s.initializationOptions,
                        settings = s.settings
                    )
                }.toJson())
            }

            else -> Err("Unknown settings category: $category")
        }.toWasmResult()

        write(retPtr, result.toBuffer())
    }

    private inline fun <reified T> T?.toJson() = json.encodeToString(this)
    private inline fun <reified T> T?.toJsonElement() = json.encodeToJsonElement(this)
}
