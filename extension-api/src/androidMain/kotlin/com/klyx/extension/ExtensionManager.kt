package com.klyx.extension

import androidx.compose.runtime.mutableStateListOf
import com.blankj.utilcode.util.FileUtils
import com.klyx.core.Environment
import com.klyx.core.extension.Extension
import com.klyx.core.extension.ExtensionToml
import com.klyx.core.extension.parseExtension
import com.klyx.core.extension.parseToml
import com.klyx.core.file.KxFile
import com.klyx.core.file.extractZip
import com.klyx.core.file.find
import com.klyx.core.file.rawFile
import com.klyx.core.file.resolve
import com.klyx.core.file.toKxFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSource
import java.io.File
import java.io.IOException
import java.nio.file.Files

object ExtensionManager {
    val installedExtensions = mutableStateListOf<Extension>()

    suspend fun installExtension(
        dir: KxFile,
        isDevExtension: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val tomlFile = dir.find("extension.toml")
            ?: return@withContext Result.failure(Exception("extension.toml not found"))

        val toml = tomlFile.inputStream()?.use {
            parseToml(it.asSource())
        } ?: return@withContext Result.failure(Exception("Failed to read extension.toml"))

        val internalDir = File(
            if (isDevExtension) Environment.DevExtensionsDir else Environment.ExtensionsDir,
            toml.id
        ).toKxFile()

        if (internalDir.exists) {
            internalDir.deleteRecursively()
        }

        internalDir.mkdirs()
        runCatching { copyRecursive(dir, internalDir) }.onFailure {
            return@withContext Result.failure(it)
        }

        runCatching {
            val ext = parseExtension(internalDir, toml).copy(isDevExtension = isDevExtension)
            installedExtensions.removeIf { it.toml.id == ext.toml.id }
            installedExtensions.add(ext)
            try {
                ExtensionLoader.loadExtension(ext, shouldCallInit = true)
            } catch (err: Exception) {
                installedExtensions.removeIf { it.toml.id == ext.toml.id }
                internalDir.deleteRecursively()
                return@withContext Result.failure(err)
            }
        }
    }

    suspend fun installExtensionFromZip(
        zipFile: KxFile,
        isDevExtension: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!zipFile.exists || !zipFile.name.endsWith(".zip")) {
            return@withContext Result.failure(IllegalArgumentException("Invalid ZIP file"))
        }

        val tempDir = Files.createTempDirectory("extension_tmp_").toFile()
        try {
            zipFile.extractZip(tempDir)
        } catch (e: Exception) {
            return@withContext Result.failure(IOException("Failed to unzip extension", e))
        }

        val result = installExtension(tempDir.toKxFile(), isDevExtension)
        tempDir.deleteRecursively()
        result
    }

    fun findExtension(id: String) = installedExtensions.find { it.toml.id == id }

    fun uninstallExtension(toml: ExtensionToml) {
        val ext = installedExtensions.find { it.toml == toml } ?: return
        if (!File(ext.path).deleteRecursively()) {
            FileUtils.delete(ext.path)
        }
        installedExtensions.remove(ext)
    }

    suspend fun loadExtensions() = withContext(Dispatchers.IO) {
        listOf(
            Environment.ExtensionsDir to false,
            Environment.DevExtensionsDir to true
        ).forEach { (dirPath, isDev) ->
            val dir = KxFile(dirPath)
            if (!dir.exists) dir.rawFile().mkdirs()
            dir.listFiles { it.isDirectory }?.forEach {
                loadExtension(it, isDev)
            }
        }
    }

    private suspend fun loadExtension(
        file: KxFile,
        isDevExtension: Boolean
    ) {
        file.resolve("extension.toml").inputStream()?.use {
            val toml = parseToml(it.asSource())
            val ext = parseExtension(file, toml).copy(isDevExtension = isDevExtension)
            installedExtensions.add(ext)
            ExtensionLoader.loadExtension(ext, true)
        }
    }

    private fun copyRecursive(source: KxFile, dest: KxFile) {
        source.listFiles()?.forEach { file ->
            val destFile = dest.resolve(file.name)
            if (file.isDirectory) {
                destFile.mkdirs()
                copyRecursive(file, destFile)
            } else {
                file.inputStream()?.use { input ->
                    destFile.outputStream()?.use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}
