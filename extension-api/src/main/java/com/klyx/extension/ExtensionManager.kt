package com.klyx.extension

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.blankj.utilcode.util.FileUtils
import com.klyx.core.Env
import com.klyx.core.extension.Extension
import com.klyx.core.extension.ExtensionToml
import com.klyx.core.extension.parseExtension
import com.klyx.core.extension.parseToml
import com.klyx.core.file.FileWrapper
import com.klyx.core.file.find
import com.klyx.core.file.inputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object ExtensionManager {
    val installedExtensions = mutableStateListOf<Extension>()

    suspend fun installExtension(
        context: Context,
        dir: FileWrapper,
        factory: ExtensionFactory,
        isDevExtension: Boolean = false
    ): Result<Any> = withContext(Dispatchers.IO) {
        val tomlFile = dir.find("extension.toml")
            ?: return@withContext Result.failure(Exception("extension.toml not found in selected folder"))

        val toml = tomlFile.inputStream(context)?.use { input ->
            try {
                parseToml(input)
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        } ?: return@withContext Result.failure(Exception("Failed to read extension.toml"))

        val internalDir = File(
            if (isDevExtension) Env.DEV_EXTENSIONS_DIR else Env.EXTENSIONS_DIR,
            toml.id
        )

        if (internalDir.exists()) {
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

        fun copyRecursive(source: FileWrapper, dest: File) {
            source.listFiles()?.forEach { file ->
                try {
                    val destFile = File(dest, file.name)

                    if (file.isDirectory) {
                        destFile.mkdirs()
                        copyRecursive(file, destFile)
                    } else {
                        file.inputStream(context)?.use { input ->
                            destFile.outputStream().use { output ->
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
        val extensionsDir = File(Env.EXTENSIONS_DIR)
        if (!extensionsDir.exists()) extensionsDir.mkdirs()

        extensionsDir.listFiles { file -> file.isDirectory }?.forEach { file ->
            val toml = parseToml(file.resolve("extension.toml").inputStream())
            val extension = parseExtension(file, toml)
            installedExtensions.add(extension)
            factory.loadExtension(extension)
        }

        val devExtensionsDir = File(Env.DEV_EXTENSIONS_DIR)
        if (!devExtensionsDir.exists()) devExtensionsDir.mkdirs()

        devExtensionsDir.listFiles { file -> file.isDirectory }?.forEach { file ->
            val toml = parseToml(file.resolve("extension.toml").inputStream())
            val extension = parseExtension(file, toml).copy(isDevExtension = true)
            installedExtensions.add(extension)
            factory.loadExtension(extension)
        }
    }
}
