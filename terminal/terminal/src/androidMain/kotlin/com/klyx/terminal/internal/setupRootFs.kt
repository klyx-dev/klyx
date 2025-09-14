package com.klyx.terminal.internal

import android.content.Context
import com.klyx.core.file.downloadFile
import com.klyx.core.logging.logger
import com.klyx.terminal.klyxBinDir
import com.klyx.terminal.klyxFilesDir
import com.klyx.terminal.ubuntuDir
import io.ktor.client.content.ProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File

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
    val rootFsPath = "${context.cacheDir.absolutePath}/ubuntu.tar.gz"
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

    if (!ubuntuDir.exists()) ubuntuDir.mkdirs()
    if (!extractTarGz(path, ubuntuDir.absolutePath)) {
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

    ubuntuDir.resolve("etc/hostname").writeText("klyx")
    ubuntuDir.resolve("etc/hosts").writeText(
        """
        127.0.0.1   localhost
        127.0.1.1   klyx
    """.trimIndent()
    )
    ubuntuDir.resolve("etc/resolv.conf").writeText("nameserver 8.8.8.8")
}

context(context: Context)
@Suppress("TooGenericExceptionThrown")
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

    try {
        dest.mkdirs()
        tarGz.inputStream().use { fis ->
            GzipCompressorInputStream(fis).use { gis ->
                TarArchiveInputStream(gis).use { tis ->
                    var entry = tis.nextEntry
                    while (entry != null) {
                        val outputFile = File(dest, entry.name)

                        if (entry.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            outputFile.parentFile?.mkdirs()
                            outputFile.outputStream().use { fos ->
                                tis.copyTo(fos)
                            }
                        }

                        entry = tis.nextEntry
                    }
                }
            }
        }
        true
    } catch (err: Throwable) {
        logger.error(err) { err.message ?: "Tar extraction failed" }
        false
    }
}
