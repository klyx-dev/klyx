package com.klyx.extension

import androidx.compose.runtime.mutableStateListOf
import com.blankj.utilcode.util.FileUtils
import com.klyx.core.Environment
import com.klyx.core.extension.Extension
import com.klyx.core.extension.ExtensionToml
import com.klyx.core.extension.parseExtension
import com.klyx.core.extension.parseToml
import com.klyx.core.file.KxFile
import com.klyx.core.file.find
import com.klyx.core.file.rawFile
import com.klyx.core.file.resolve
import com.klyx.core.file.toKxFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSource
import java.io.File
import java.io.IOException

object ExtensionManager {
    val installedExtensions = mutableStateListOf<Extension>()

    suspend fun installExtension(
        dir: KxFile,
        factory: ExtensionFactory,
        isDevExtension: Boolean = false
    ): Result<Any> = withContext(Dispatchers.IO) {
        val tomlFile = dir.find("extension.toml")
            ?: return@withContext Result.failure(Exception("extension.toml not found in selected folder"))

        val tomlInput = tomlFile.inputStream() ?: return@withContext Result.failure(Exception("Failed to read extension.toml"))

        val toml = tomlInput.use { input ->
            try {
                parseToml(input.asSource())
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

        val internalDir = File(
            if (isDevExtension) Environment.DevExtensionsDir else Environment.ExtensionsDir,
            toml.id
        ).toKxFile()

        if (internalDir.exists) {
            runCatching {
                val extension = parseExtension(internalDir, toml).copy(isDevExtension = isDevExtension)
                installedExtensions.add(extension)
                factory.loadExtension(extension)
                return@withContext Result.success(Unit)
            }.onFailure {
                return@withContext Result.failure(it)
            }
        }

        internalDir.mkdirs()

        fun copyRecursive(source: KxFile, dest: KxFile) {
            source.listFiles()?.forEach { file ->
                try {
                    val destFile = dest.resolve(file.name)

                    if (file.isDirectory) {
                        destFile.mkdirs()
                        copyRecursive(file, destFile)
                    } else {
                        val inputStream = file.inputStream()

                        inputStream?.use { input ->
                            destFile.outputStream()?.use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                } catch (e: IOException) {
                    throw e
                }
            }
        }

        runCatching { copyRecursive(dir, internalDir) }.onFailure {
            return@withContext Result.failure(it)
        }

        runCatching {
            val extension = parseExtension(internalDir, toml).copy(isDevExtension = isDevExtension)
            installedExtensions.add(extension)
            factory.loadExtension(extension)
        }.onFailure {
            return@withContext Result.failure(it)
        }

        Result.success(Unit)
    }

    fun uninstallExtension(toml: ExtensionToml) {
        val extension = installedExtensions.find { it.toml == toml } ?: return

        if (!File(extension.path).deleteRecursively()) {
            FileUtils.delete(extension.path)
        }

        installedExtensions.remove(extension)
    }

    fun findExtension(id: String): Extension? {
        return installedExtensions.find { it.toml.id == id }
    }

    suspend fun loadExtensions(factory: ExtensionFactory) = withContext(Dispatchers.IO) {
        val extensionsDir = KxFile(Environment.ExtensionsDir)
        if (!extensionsDir.exists) extensionsDir.rawFile().mkdirs()

        extensionsDir.listFiles { it.isDirectory }?.forEach { loadExtension(it, factory) }

        val devExtensionsDir = KxFile(Environment.DevExtensionsDir)
        if (!devExtensionsDir.exists) devExtensionsDir.rawFile().mkdirs()

        devExtensionsDir.listFiles { it.isDirectory }?.forEach { loadExtension(it, factory, true) }
    }

    private suspend fun loadExtension(
        file: KxFile,
        factory: ExtensionFactory,
        isDevExtension: Boolean = false
    ) {
        val input = file.resolve("extension.toml").inputStream()

        if (input != null) {
            val toml = parseToml(input.asSource())
            val extension = parseExtension(file, toml).copy(isDevExtension = isDevExtension)
            installedExtensions.add(extension)
            factory.loadExtension(extension)
        }
    }
}
