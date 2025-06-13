package com.klyx.extension

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.blankj.utilcode.util.FileUtils
import com.klyx.core.Env
import com.klyx.core.file.DocumentFileWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object ExtensionManager {
    val installedExtensions = mutableStateListOf<Extension>()

    suspend fun installExtension(
        context: Context,
        dir: DocumentFileWrapper,
        factory: ExtensionFactory,
        isDevExtension: Boolean = false,
        onError: (String, IOException) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val tomlFile = dir.raw.findFile("extension.toml")
            ?: throw IOException("extension.toml not found in selected folder")

        val toml = context.contentResolver.openInputStream(tomlFile.uri)?.use { input ->
            parseToml(input)
        } ?: throw IOException("Failed to read extension.toml")

        val internalDir = File(
            if (isDevExtension) Env.DEV_EXTENSIONS_DIR else Env.EXTENSIONS_DIR,
            toml.id
        )

        if (internalDir.exists()) internalDir.deleteRecursively()
        internalDir.mkdirs()

        fun copyRecursive(source: DocumentFileWrapper, dest: File) {
            source.listFiles().forEach { file ->
                try {
                    val destFile = File(dest, file.name)

                    if (file.isDirectory) {
                        destFile.mkdirs()
                        copyRecursive(file, destFile)
                    } else {
                        context.contentResolver.openInputStream(file.uri(context))?.use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                } catch (e: IOException) {
                    onError(file.name, e)
                    throw e
                }
            }
        }

        copyRecursive(dir, internalDir)

        val extension = parseExtension(internalDir, toml).copy(isDevExtension = isDevExtension)
        installedExtensions.add(extension)
        factory.loadExtension(extension)
    }

    fun uninstallExtension(extension: Extension) {
        if (!File(extension.path).deleteRecursively()) {
            FileUtils.delete(extension.path)
        }

        installedExtensions.remove(extension)
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
