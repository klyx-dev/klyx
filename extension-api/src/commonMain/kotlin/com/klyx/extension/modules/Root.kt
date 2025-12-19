@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.modules

import arrow.core.getOrElse
import arrow.core.toOption
import com.klyx.core.Notifier
import com.klyx.core.extension.WorktreeDelegate
import com.klyx.core.file.archive.extractGzip
import com.klyx.core.file.archive.extractGzipTar
import com.klyx.core.file.archive.extractZip
import com.klyx.core.file.humanBytes
import com.klyx.core.file.toKotlinxIoPath
import com.klyx.core.io.intoPath
import com.klyx.core.io.resolveToSandbox
import com.klyx.core.logging.KxLogger
import com.klyx.core.settings.LspSettings
import com.klyx.core.settings.SettingsManager
import com.klyx.extension.api.DownloadedFileType
import com.klyx.extension.api.lsp.CommandSettings
import com.klyx.extension.api.lsp.LanguageServerInstallationStatus
import com.klyx.extension.api.lsp.parseLanguageServerInstallationStatus
import com.klyx.extension.api.parseSettingsLocation
import com.klyx.extension.internal.asOption
import com.klyx.extension.internal.toWasmOption
import com.klyx.extension.internal.toWasmResult
import com.klyx.core.internal.pointer.asPointer
import com.klyx.core.internal.pointer.dropPtr
import com.klyx.core.internal.pointer.value
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.annotations.HostFunction
import com.klyx.wasm.annotations.HostModule
import com.klyx.wasm.type.WasmUnit
import com.klyx.wasm.type.collections.toWasmList
import com.klyx.wasm.type.toBuffer
import com.klyx.wasm.type.toWasm
import com.klyx.wasm.type.wstr
import io.itsvks.anyhow.Err
import io.itsvks.anyhow.Ok
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
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
    suspend fun WasmMemory.worktreeReadTextFile(ptr: Int, path: String, retPtr: Int) {
        val worktree = ptr.asPointer().value<WorktreeDelegate>()
        val result = worktree.readTextFile(path.intoPath()).toWasmResult()
        write(retPtr, result.toBuffer())
    }

    @HostFunction("[method]worktree.id")
    fun worktreeId(ptr: Int) = ptr.asPointer().value<WorktreeDelegate>().id()

    @HostFunction("[method]worktree.root-path")
    fun WasmMemory.worktreeRootPath(ptr: Int, retPtr: Int) {
        val worktree = ptr.asPointer().value<WorktreeDelegate>()
        val rootPath = worktree.rootPath().toWasm()
        write(retPtr, rootPath.toBuffer())
    }

    @HostFunction("[method]worktree.which")
    suspend fun WasmMemory.worktreeWhich(ptr: Int, binaryName: String, retPtr: Int) {
        val worktree = ptr.asPointer().value<WorktreeDelegate>()
        val result = worktree.which(binaryName).toWasmOption()
        write(retPtr, result.toBuffer())
    }

    @HostFunction("[method]worktree.shell-env")
    suspend fun WasmMemory.worktreeShellEnv(ptr: Int, retPtr: Int) {
        val worktree = ptr.asPointer().value<WorktreeDelegate>()
        val envVars = worktree.shellEnv().toList().toWasmList()
        write(retPtr, envVars.toBuffer())
    }

    @HostFunction
    fun WasmMemory.makeFileExecutable(path: String, resultPtr: Int) {
        val res = com.klyx.extension.internal.makeFileExecutable(path.resolveToSandbox()).toWasmResult()
        write(resultPtr, res.toBuffer())
    }

    @HostFunction
    @Deprecated("deprecated (available in `klyx_extension_api <= 1.3.4`)")
    suspend fun WasmMemory.downloadFile(url: String, path: String, resultPtr: Int) {
        downloadFile(url, path, DownloadedFileType.Uncompressed.value, resultPtr)
    }

    @HostFunction
    suspend fun WasmMemory.downloadFile(
        url: String,
        path: String,
        downloadedFileTypeValue: Int,
        resultPtr: Int
    ) {
        val fileType = DownloadedFileType.fromValue(downloadedFileTypeValue)
        val extractTarget = path.resolveToSandbox().toPath()

        val result = try {
            FileSystem.SYSTEM.createDirectories(extractTarget.parent ?: extractTarget)

            val tempArchive = when (fileType) {
                DownloadedFileType.Uncompressed -> extractTarget
                DownloadedFileType.GZip -> extractTarget.parent!! / (extractTarget.name + ".tmp.gz")
                DownloadedFileType.GZipTar -> extractTarget.parent!! / (extractTarget.name + ".tmp.tar.gz")
                DownloadedFileType.Zip -> extractTarget.parent!! / (extractTarget.name + ".tmp.zip")
            }

            // download to temporary file
            com.klyx.core.file.downloadFile(
                url = url,
                outputPath = tempArchive.toString(),
                onDownload = { total, length ->
                    val progress = if (length == null) 0f else total.toFloat() / length
                    logger.progress((progress * 100).toInt()) {
                        "(${total.humanBytes()} / ${length?.humanBytes()}) Downloading $url"
                    }
                },
                onComplete = {
                    when (fileType) {

                        DownloadedFileType.Uncompressed -> {
                            // already downloaded directly to final location
                        }

                        DownloadedFileType.GZip -> {
                            logger.info { "Extracting GZip -> $extractTarget" }
                            extractGzip(tempArchive.toKotlinxIoPath(), extractTarget.toKotlinxIoPath())
                            FileSystem.SYSTEM.delete(tempArchive)
                        }

                        DownloadedFileType.GZipTar -> {
                            logger.info { "Extracting Tar.Gz -> $extractTarget" }
                            FileSystem.SYSTEM.createDirectories(extractTarget)
                            extractGzipTar(tempArchive.toKotlinxIoPath(), extractTarget.toKotlinxIoPath())
                            FileSystem.SYSTEM.delete(tempArchive)
                        }

                        DownloadedFileType.Zip -> {
                            logger.info { "Extracting Zip -> $extractTarget" }
                            FileSystem.SYSTEM.createDirectories(extractTarget)
                            extractZip(tempArchive.toKotlinxIoPath(), extractTarget.toKotlinxIoPath())
                            FileSystem.SYSTEM.delete(tempArchive)
                        }
                    }
                }
            )

            WasmOk(WasmUnit)
        } catch (e: Exception) {
            WasmErr(e.toString().wstr)
        }

        write(resultPtr, result.toBuffer())
    }

    @HostFunction
    suspend fun WasmMemory.unzipFile(zipPath: String, destPath: String, resultPtr: Int) {
        val result = try {
            com.klyx.core.file.unzip(zipPath.toPath(), destPath.toPath())
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
                        settings?.tabSize?.let { put("tab_size", it.toInt()) }
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
