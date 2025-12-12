package com.klyx.extension

import androidx.compose.runtime.mutableStateListOf
import com.klyx.core.extension.Extension
import com.klyx.core.extension.ExtensionInfo
import com.klyx.core.extension.parseExtension
import com.klyx.core.extension.parseToml
import com.klyx.core.file.KxFile
import com.klyx.core.file.find
import com.klyx.core.file.okioSource
import com.klyx.core.file.resolve
import com.klyx.core.file.source
import com.klyx.core.file.toKxFile
import com.klyx.core.file.toOkioPath
import com.klyx.core.io.Paths
import com.klyx.core.io.extensionsDir
import com.klyx.core.io.isSymlink
import com.klyx.core.logging.logger
import com.klyx.core.lsp.languageIdentifiers
import com.klyx.core.settings.AppSettings
import com.klyx.wasm.ExperimentalWasmApi
import io.itsvks.anyhow.Err
import io.itsvks.anyhow.Ok
import io.itsvks.anyhow.anyhow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
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

    private val _loadingState = MutableStateFlow(0 to 0) // (loaded, total)
    val loadingState = _loadingState.asStateFlow()

    private val fs = FileSystem.SYSTEM
    private val logger = logger()

    @OptIn(ExperimentalWasmApi::class)
    suspend fun installExtension(directory: KxFile, isDevExtension: Boolean = false) = anyhow {
        withContext(Dispatchers.IO) {
            val tomlFile = directory.find("extension.toml") ?: bail("extension.toml not found")
            val info = withContext(Dispatchers.Default) { parseToml(tomlFile.source()) }

            val extensionDirectory = Paths.extensionsDir.toString()
            val installDirectory = KxFile(extensionDirectory, info.id)

            if (!installDirectory.exists) {
                installDirectory.mkdirs()
                try {
                    copyDir(directory.toOkioPath(), installDirectory.toOkioPath())
                } catch (err: Exception) {
                    logger.error(err) { err.message }
                    bail(err.message ?: "Failed to copy directory.")
                }
            }

            try {
                val extension = withContext(Dispatchers.Default) {
                    parseExtension(installDirectory, info).copy(isDevExtension = isDevExtension)
                }

                installedExtensions.removeAll { it.info.id == extension.info.id }
                loadedExtensions.removeAll { it.extension.info.id == extension.info.id }
                installedExtensions.add(extension)

                val localExtension = ExtensionLoader.loadExtension(extension, true)
                if (localExtension != null) loadedExtensions.add(localExtension)
            } catch (err: Exception) {
                logger.error(err) { err.message }
                installedExtensions.removeAll { it.info.id == info.id }
                loadedExtensions.removeAll { it.extension.info.id == info.id }
                bail(err.message ?: "Failed to load extension.")
            }
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
        return loadedExtensions.any { extension ->
            extension.extension.info.languageServers.values.any { serverConfig ->
                languageName in serverConfig.languages || languageName.equals(serverConfig.language, ignoreCase = true)
            }
        }
    }

    fun getLanguageServerIdForLanguage(languageName: String, settings: AppSettings): String? {
        settings.languages[languageName]?.let { languageSettings ->
            (languageSettings["language_servers"] as? JsonArray)?.let { languageServers ->
                return runCatching { languageServers.firstOrNull()?.jsonPrimitive?.contentOrNull }.getOrNull()
            }
        }

        return loadedExtensions.firstNotNullOfOrNull { extension ->
            extension.extension.info.languageServers.entries.firstOrNull { (_, serverConfig) ->
                languageName in serverConfig.languages ||
                        (serverConfig.languageIds?.keys?.contains(languageName) == true) ||
                        languageName.equals(serverConfig.language, ignoreCase = true)
            }?.key
        }
    }

    fun getLanguageIdForLanguage(languageName: String): String? {
        return loadedExtensions.firstNotNullOfOrNull { extension ->
            extension.extension.info.languageServers.values.firstNotNullOfOrNull { serverConfig ->
                serverConfig.languageIds?.get(languageName) ?: if (
                    languageName in serverConfig.languages ||
                    languageName.equals(serverConfig.language, ignoreCase = true)
                ) {
                    languageIdentifiers[languageName] ?: languageName.lowercase()
                } else null
            }
        } ?: languageIdentifiers[languageName]
    }

    fun getExtensionsForLanguage(languageName: String): List<LocalExtension> {
        return loadedExtensions.filter { extension ->
            extension.extension.info.languageServers.values.any { serverConfig ->
                languageName in serverConfig.languages || languageName.equals(serverConfig.language, ignoreCase = true)
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
            val directory = Paths.extensionsDir.toOkioPath()
            if (!fs.exists(directory)) fs.createDirectories(directory)
            val dirs = fs.list(directory)
                .filter { fs.metadata(it).isDirectory }

            _loadingState.update { 0 to dirs.size }

            coroutineScope {
                dirs.chunked(4).map { chunk ->
                    async {
                        chunk.map { child ->
                            async(Dispatchers.IO) {
                                try {
                                    loadExtension(child.toKxFile(), child.isSymlink())
                                } finally {
                                    _loadingState.update { (loaded, total) -> (loaded + 1) to total }
                                }
                            }
                        }.awaitAll()
                    }
                }.awaitAll()
            }

            Ok(Unit)
        } catch (err: Exception) {
            logger.error(err) { err.message }
            Err(err.message ?: "Failed to load extensions.")
        }
    }

    @OptIn(ExperimentalWasmApi::class)
    private suspend fun loadExtension(directory: KxFile, isDevExtension: Boolean) {
        val tomlFileSource = directory.resolve("extension.toml").source()
        val info = withContext(Dispatchers.Default) {
            parseToml(tomlFileSource)
        }

        tomlFileSource.close()

        val extension = withContext(Dispatchers.Default) {
            parseExtension(directory, info).copy(isDevExtension = isDevExtension)
        }
        installedExtensions.add(extension)

        ExtensionLoader.loadExtension(extension, true)?.let {
            loadedExtensions.add(it)
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
