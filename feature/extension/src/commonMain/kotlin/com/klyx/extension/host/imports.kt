package com.klyx.extension.host

import com.klyx.core.file.archive.extractGzip
import com.klyx.core.file.archive.extractGzipTar
import com.klyx.core.file.archive.extractZip
import com.klyx.core.file.humanBytes
import com.klyx.core.file.toKotlinxIoPath
import com.klyx.core.io.okioFs
import com.klyx.core.io.resolveToSandbox
import com.klyx.core.logging.log
import com.klyx.core.settings.SettingsManager
import com.klyx.editor.language.BinaryStatus
import com.klyx.extension.CommandSettings
import com.klyx.extension.ExtensionHostProxy
import com.klyx.extension.LspSettings
import com.klyx.extension.native.DownloadedFileType
import com.klyx.extension.native.ExtensionImports
import com.klyx.extension.native.ExtensionRuntimeException
import com.klyx.extension.native.LanguageServerInstallationStatus
import com.klyx.extension.native.SettingsImportException
import com.klyx.extension.native.SettingsLocation
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okio.FileSystem
import okio.Path.Companion.toPath

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    namingStrategy = JsonNamingStrategy.SnakeCase
    isLenient = true
    explicitNulls = false
    encodeDefaults = true
    prettyPrint = true
    prettyPrintIndent = "  "
}

fun ExtensionImports(proxy: ExtensionHostProxy) = object : ExtensionImports {
    override suspend fun setLanguageServerInstallationStatus(
        serverName: String,
        status: LanguageServerInstallationStatus
    ) {
        val status = when (status) {
            LanguageServerInstallationStatus.CheckingForUpdate -> BinaryStatus.CheckingForUpdate
            LanguageServerInstallationStatus.Downloading -> BinaryStatus.Downloading
            is LanguageServerInstallationStatus.Failed -> BinaryStatus.Failed(status.v1)
            LanguageServerInstallationStatus.None -> BinaryStatus.None
        }
        proxy.updateLanguageServerStatus(serverName, status)
    }

    override suspend fun getSettings(
        location: SettingsLocation?,
        category: String,
        key: String?
    ): String {
        val allSettings = SettingsManager.settings.value
        val defaultSettings = SettingsManager.defaultSettings

        return when (category) {
            "language" -> {
                val languageSettings = key?.let { allSettings.languages[it] }

                val s = languageSettings?.let { settings ->
                    try {
                        buildJsonObject {
                            put("tab_size", settings.tabSize.toInt())
                        }.toJson()
                    } catch (t: Throwable) {
                        throw SettingsImportException.SerializationException(t.stackTraceToString())
                    }
                }

                s ?: throw SettingsImportException.Internal("Language setting not found: $key")
            }

            "lsp" -> {
                val lspSettings = key?.let {
                    allSettings.lsp[it] ?: defaultSettings.lsp[it]
                } ?: com.klyx.core.settings.LspSettings()

                try {
                    lspSettings.let { s ->
                        LspSettings(
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
                    }.toJson()
                } catch (t: Throwable) {
                    throw SettingsImportException.SerializationException(t.stackTraceToString())
                }
            }

            else -> throw SettingsImportException.UnknownCategory(category)
        }
    }

    private inline fun <reified T> T?.toJson() = json.encodeToString(this)

    override suspend fun downloadFile(
        url: String,
        path: String,
        fileType: DownloadedFileType
    ) {
        val extractTarget = path.resolveToSandbox().toPath()

        try {
            okioFs.createDirectories(extractTarget.parent ?: extractTarget)

            val tempArchive = when (fileType) {
                DownloadedFileType.GZIP -> extractTarget.parent!! / (extractTarget.name + ".tmp.gz")
                DownloadedFileType.GZIP_TAR -> extractTarget.parent!! / (extractTarget.name + ".tmp.tar.gz")
                DownloadedFileType.ZIP -> extractTarget.parent!! / (extractTarget.name + ".tmp.zip")
                DownloadedFileType.UNCOMPRESSED -> extractTarget
            }

            // download to temporary file
            com.klyx.core.file.downloadFile(
                url = url,
                outputPath = tempArchive.toString(),
                onDownload = { total, length ->
                    val progress = if (length == null) 0f else total.toFloat() / length
                    log.progress((progress * 100).toInt()) {
                        "(${total.humanBytes()} / ${length?.humanBytes()}) Downloading $url"
                    }
                },
                onComplete = {
                    when (fileType) {

                        DownloadedFileType.UNCOMPRESSED -> {
                            // already downloaded directly to final location
                        }

                        DownloadedFileType.GZIP -> {
                            log.info { "Extracting GZip -> $extractTarget" }
                            extractGzip(tempArchive.toKotlinxIoPath(), extractTarget.toKotlinxIoPath())
                            FileSystem.SYSTEM.delete(tempArchive)
                        }

                        DownloadedFileType.GZIP_TAR -> {
                            log.info { "Extracting Tar.Gz -> $extractTarget" }
                            FileSystem.SYSTEM.createDirectories(extractTarget)
                            extractGzipTar(tempArchive.toKotlinxIoPath(), extractTarget.toKotlinxIoPath())
                            FileSystem.SYSTEM.delete(tempArchive)
                        }

                        DownloadedFileType.ZIP -> {
                            log.info { "Extracting Zip -> $extractTarget" }
                            FileSystem.SYSTEM.createDirectories(extractTarget)
                            extractZip(tempArchive.toKotlinxIoPath(), extractTarget.toKotlinxIoPath())
                            FileSystem.SYSTEM.delete(tempArchive)
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            throw ExtensionRuntimeException(t.stackTraceToString())
        }
    }
}
