package com.klyx.extension

import androidx.compose.runtime.mutableStateListOf
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.klyx.core.Environment
import com.klyx.core.extension.Extension
import com.klyx.core.extension.ExtensionInfo
import com.klyx.core.extension.parseExtension
import com.klyx.core.extension.parseToml
import com.klyx.core.file.KxFile
import com.klyx.core.file.find
import com.klyx.core.file.okioSource
import com.klyx.core.file.resolve
import com.klyx.core.file.toKxFile
import com.klyx.core.file.toOkioPath
import com.klyx.core.logging.logger
import com.klyx.core.lsp.languageIdentifiers
import com.klyx.wasm.ExperimentalWasmApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.openZip
import okio.use

object ExtensionManager {
    val installedExtensions = mutableStateListOf<Extension>()
    val loadedExtensions = mutableStateListOf<LocalExtension>()

    private val fs = FileSystem.SYSTEM
    private val logger = logger()

    @OptIn(ExperimentalWasmApi::class)
    suspend fun installExtension(directory: KxFile, isDevExtension: Boolean = false) = withContext(Dispatchers.IO) {
        val tomlFile = directory.find("extension.toml") ?: return@withContext Err("extension.toml not found")
        val info = parseToml(tomlFile.source())

        val extensionDirectory = if (isDevExtension) Environment.DevExtensionsDir else Environment.ExtensionsDir
        val installDirectory = KxFile(extensionDirectory, info.id)

        if (!installDirectory.exists) {
            installDirectory.mkdirs()
            try {
                copyDir(directory.toOkioPath(), installDirectory.toOkioPath())
            } catch (err: Exception) {
                logger.error(err) { err.message }
                return@withContext Err(err.message ?: "Failed to copy directory.")
            }
        }

        try {
            val extension = parseExtension(installDirectory, info).copy(isDevExtension = isDevExtension)
            installedExtensions.removeAll { it.info.id == extension.info.id }
            loadedExtensions.removeAll { it.extension.info.id == extension.info.id }
            installedExtensions.add(extension)

            val localExtension = ExtensionLoader.loadExtension(extension, true)
            if (localExtension != null) loadedExtensions.add(localExtension)
            Ok(Unit)
        } catch (err: Exception) {
            logger.error(err) { err.message }
            installedExtensions.removeAll { it.info.id == info.id }
            loadedExtensions.removeAll { it.extension.info.id == info.id }
            Err(err.message ?: "Failed to load extension.")
        }
    }

    suspend fun installExtensionFromZip(
        zipFile: KxFile,
        isDevExtension: Boolean = false
    ) = withContext(Dispatchers.IO) {
        if (!zipFile.exists || !zipFile.name.endsWith(".zip")) {
            return@withContext Err("Invalid ZIP file")
        }

        val tmpDirectory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "extension_tmp_${zipFile.name}"
        val tmpZip = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "extension_tmp.zip"
        try {
            zipFile.okioSource().use { source ->
                fs.sink(tmpZip).buffer().use { sink ->
                    sink.writeAll(source)
                }
            }

            fs.createDirectories(tmpDirectory)
            unzip(tmpZip, tmpDirectory)
        } catch (e: Exception) {
            logger.error(e) { e.message }
            return@withContext Err("Failed to unzip extension: ${e.message}")
        }

        installExtension(tmpDirectory.toKxFile(), isDevExtension).also {
            fs.deleteRecursively(tmpDirectory)
        }
    }

    fun findInstalledExtension(id: String) = installedExtensions.find { it.info.id == id }
    fun findLoadedExtension(id: String) = loadedExtensions.find { it.extension.info.id == id }

    fun isExtensionAvailableForLanguage(languageName: String): Boolean {
        println(languageName)
        println(loadedExtensions.map { it.extension.info }.joinToString())
        return loadedExtensions.any { extension ->
            extension.extension.info.languageServers.values.any { serverConfig ->
                languageName in serverConfig.languages
            }
        }
    }

    fun getServerIdForLanguage(languageName: String): String? {
        return loadedExtensions.firstNotNullOfOrNull { extension ->
            extension.extension.info.languageServers.entries.firstOrNull { (_, serverConfig) ->
                languageName in serverConfig.languages || (serverConfig.languageIds?.keys?.contains(languageName) == true)
            }?.key
        }
    }

    fun getLanguageIdForLanguage(languageName: String): String? {
        return loadedExtensions.firstNotNullOfOrNull { extension ->
            extension.extension.info.languageServers.values.firstNotNullOfOrNull { serverConfig ->
                serverConfig.languageIds?.get(languageName) ?: if (languageName in serverConfig.languages) {
                    languageIdentifiers[languageName] ?: languageName.lowercase()
                } else null
            }
        } ?: languageIdentifiers[languageName]
    }

    fun getExtensionsForLanguage(languageName: String): List<LocalExtension> {
        return loadedExtensions.filter { extension ->
            extension.extension.info.languageServers.values.any { serverConfig ->
                languageName in serverConfig.languages
            }
        }
    }

    fun getExtensionForLanguage(languageName: String) = getExtensionsForLanguage(languageName).firstOrNull()

    fun uninstallExtension(info: ExtensionInfo) {
        val extension = installedExtensions.find { it.info == info } ?: return
        runCatching { fs.deleteRecursively(extension.path.toPath()) }.onFailure {
            logger.error(it) { it.message }
        }
        installedExtensions.remove(extension)

        findLoadedExtension(info.id)?.let {
            it.dispose()
            loadedExtensions.remove(it)
        }
    }

    suspend fun loadExtensions() = withContext(Dispatchers.IO) {
        try {
            listOf(
                Environment.ExtensionsDir to false,
                Environment.DevExtensionsDir to true
            ).forEach { (dirPath, isDev) ->
                val directory = dirPath.toPath()
                if (!fs.exists(directory)) {
                    fs.createDirectories(directory)
                }

                fs.list(directory).forEach { child ->
                    if (fs.metadata(child).isDirectory) {
                        loadExtension(child.toKxFile(), isDev)
                    }
                }
            }
            Ok(Unit)
        } catch (err: Exception) {
            logger.error(err) { err.message }
            Err(err.message ?: "Failed to load extensions.")
        }
    }

    @OptIn(ExperimentalWasmApi::class)
    private suspend fun loadExtension(directory: KxFile, isDevExtension: Boolean) {
        directory.resolve("extension.toml").source().use { tomlSource ->
            val info = parseToml(tomlSource)
            val extension = parseExtension(directory, info).copy(isDevExtension = isDevExtension)
            installedExtensions.add(extension)

            ExtensionLoader.loadExtension(extension, true)?.let {
                loadedExtensions.add(it)
            }
        }
    }

    private suspend fun copyDir(src: Path, dest: Path) {
        withContext(Dispatchers.IO) {
            fs.createDirectories(dest)

            fs.list(src).forEach { child ->
                val target = dest / child.name
                if (fs.metadata(child).isDirectory) {
                    copyDir(child, target)
                } else {
                    fs.createDirectories(target.parent!!)
                    fs.source(child).use { input ->
                        fs.sink(target).use { output ->
                            val buffer = Buffer()
                            while (true) {
                                val read = input.read(buffer, 8192)
                                if (read == -1L) break
                                output.write(buffer, read)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun unzip(zipPath: Path, destDir: Path) = withContext(Dispatchers.IO) {
        fs.createDirectories(destDir)

        fs.openZip(zipPath).use { zipFs ->
            for (entry in zipFs.listRecursively("/".toPath())) {
                val target = destDir / entry.toString().removePrefix("/")
                val metadata = zipFs.metadataOrNull(entry)

                if (metadata?.isDirectory == true) {
                    fs.createDirectories(target)
                } else {
                    fs.createDirectories(target.parent!!)
                    zipFs.source(entry).use { input ->
                        fs.sink(target).use { output ->
                            input.buffer().readAll(output)
                        }
                    }
                }
            }
        }
    }
}
