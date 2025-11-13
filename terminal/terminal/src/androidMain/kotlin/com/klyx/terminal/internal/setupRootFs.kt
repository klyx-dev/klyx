package com.klyx.terminal.internal

import android.content.Context
import android.system.Os
import com.klyx.core.file.downloadFile
import com.klyx.core.logging.logger
import com.klyx.terminal.klyxBinDir
import com.klyx.terminal.klyxFilesDir
import com.klyx.terminal.sandboxDir
import io.ktor.client.content.ProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileNotFoundException

val ubuntuRootFsUrl = when (PREFERRED_ABI) {
    "arm64-v8a" -> "https://cdimage.ubuntu.com/ubuntu-base/releases/plucky/release/ubuntu-base-25.04-base-arm64.tar.gz"
    "armeabi-v7a" -> "https://cdimage.ubuntu.com/ubuntu-base/releases/plucky/release/ubuntu-base-25.04-base-armhf.tar.gz"
    "x86_64" -> "https://cdimage.ubuntu.com/ubuntu-base/releases/plucky/release/ubuntu-base-25.04-base-amd64.tar.gz"
    else -> error("Unsupported ABI: $PREFERRED_ABI")
}

private val logger = logger("TerminalSetup")

context(context: Context)
suspend fun downloadRootFs(
    onDownload: ProgressListener? = null,
    onComplete: suspend (outPath: String) -> Unit = {},
    onError: suspend (error: Throwable) -> Unit = {}
) {
    val rootFsPath = "${context.cacheDir.absolutePath}/sandbox.tar.gz"
    try {
        downloadFile(
            url = ubuntuRootFsUrl,
            outputPath = rootFsPath,
            onDownload = onDownload,
            onComplete = { onComplete(rootFsPath) }
        )
    } catch (e: Exception) {
        logger.error(e) { "Failed to download rootfs" }
        onError(e)
    }
}

context(context: Context)
suspend fun setupRootFs(path: String) = run {
    fun failedToExtract(name: String): Nothing {
        error("Failed to extract $name")
    }

    if (!sandboxDir.exists()) sandboxDir.mkdirs()
    if (!extractTarGz(path, sandboxDir.absolutePath)) {
        logger.warn { "Ubuntu rootfs is not extracted properly." }
    }
    SystemFileSystem.delete(Path(path))

    downloadPackage("proot") {
        if (!extractTarGz(it.absolutePath, klyxFilesDir.absolutePath)) {
            failedToExtract("proot")
        }
    }

    downloadPackage("libtalloc") {
        if (!extractTarGz(it.absolutePath, klyxFilesDir.absolutePath)) {
            failedToExtract("libtalloc")
        }
    }

    File(klyxBinDir, "init").writeBytes(
        context.assets.open("terminal/init.sh").use { it.readBytes() }
    )

    with(klyxFilesDir.resolve("usr/tmp")) {
        if (!exists()) mkdirs()
    }
}

context(context: Context)
suspend fun downloadPackage(name: String, onComplete: suspend (File) -> Unit = {}) {
    val outFile = File(context.cacheDir, "$name.tar.gz")
    downloadFile(packageUrl(name), outFile.absolutePath, onComplete = { onComplete(outFile) })
}

suspend fun extractTarGz(
    inputPath: String,
    outputPath: String
) = withContext(Dispatchers.IO) {
    val tarGz = File(inputPath)
    val dest = File(outputPath)

    val canonicalOutputDir = dest.canonicalFile
    if (!tarGz.exists()) throw FileNotFoundException("File not found: ${tarGz.path}")

    try {
        dest.mkdirs()
        GzipCompressorInputStream(tarGz.inputStream().buffered()).use { gzIn ->
            TarArchiveInputStream(gzIn).use { tarIn ->
                generateSequence { tarIn.nextEntry }.forEach { entry ->
                    val outputFile = File(dest, entry.name).canonicalFile

                    if (!outputFile.toPath().startsWith(canonicalOutputDir.toPath())) {
                        throw SecurityException("Zip Slip vulnerability detected! Malicious entry: ${entry.name}")
                    }

                    when {
                        entry.isDirectory -> outputFile.mkdirs()

                        entry.isSymbolicLink -> {
                            try {
                                Os.symlink(entry.linkName, outputFile.absolutePath)
                            } catch (_: Exception) {
                                outputFile.parentFile?.mkdirs()
                                outputFile.outputStream().use { fos ->
                                    tarIn.copyTo(fos)
                                }
                            }
                        }

                        else -> {
                            outputFile.parentFile?.mkdirs()
                            outputFile.outputStream().use { fos ->
                                tarIn.copyTo(fos)
                            }
                        }
                    }

                    restorePermissions(outputFile, entry)
                }
            }
        }
        true
    } catch (err: Throwable) {
        logger.error(err) { err.message ?: "Tar extraction failed" }
        false
    }
}

private fun restorePermissions(file: File, entry: TarArchiveEntry) {
    val mode = entry.mode

    file.setReadable((mode and 0b100_000_000) != 0, true)
    file.setWritable((mode and 0b010_000_000) != 0, true)
    file.setExecutable((mode and 0b001_000_000) != 0, true)

    file.setReadable((mode and 0b100_000) != 0, false)
    file.setWritable((mode and 0b010_000) != 0, false)
    file.setExecutable((mode and 0b001_000) != 0, false)

    file.setReadable((mode and 0b100) != 0, false)
    file.setWritable((mode and 0b010) != 0, false)
    file.setExecutable((mode and 0b001) != 0, false)

    file.setLastModified(entry.modTime.time)

    try {
        Os.chmod(file.absolutePath, mode)
    } catch (_: Exception) {
        // ignore
    }
}
