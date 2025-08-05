package com.klyx.terminal.internal

import android.content.Context
import android.util.Log
import com.klyx.core.file.DownloadProgress
import com.klyx.core.file.downloadToWithProgress
import com.klyx.core.process
import com.klyx.terminal.klyxBinDir
import com.klyx.terminal.klyxFilesDir
import com.klyx.terminal.ubuntuDir
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import java.io.File

context(context: Context)
suspend fun downloadRootFs(
    onProgress: suspend (DownloadProgress) -> Unit = {},
    onComplete: suspend (outPath: String) -> Unit = {},
    onError: suspend (error: Throwable) -> Unit = {}
) {
    fun throwError(error: Throwable): Nothing {
        throw RuntimeException("Failed to download rootfs", error)
    }

    val rootFsPath = "${context.cacheDir.absolutePath}/ubuntu.tar.gz"
    ubuntuFlow(rootFsPath).onCompletion { error ->
        if (error != null) {
            //throwError(error)
            error.printStackTrace()
            onError(error)
        } else {
            onComplete(rootFsPath)
        }
    }.catch { error ->
        Log.e("TerminalSetup", "Error", error)
        onError(error)
    }.collect { onProgress(it) }
}

context(context: Context)
suspend fun setupRootFs(path: String) = run {
    fun failedToExtract(name: String): Nothing {
        throw RuntimeException("Failed to extract $name")
    }

    if (!ubuntuDir.exists()) ubuntuDir.mkdirs()
    if (!extractTarGz(path, ubuntuDir.absolutePath)) {
        Log.w("TerminalSetup", "Ubuntu rootfs is not extracted properly.")
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
    ubuntuDir.resolve("etc/hosts").writeText("""
        127.0.0.1   localhost
        127.0.1.1   klyx
    """.trimIndent())
    ubuntuDir.resolve("etc/resolv.conf").writeText("nameserver 8.8.8.8")
}

context(context: Context)
suspend fun downloadPackage(name: String, onComplete: suspend (File) -> Unit = {}) {
    val outFile = File(context.cacheDir, "$name.tar.gz")
    packageUrl(name).downloadToWithProgress(outFile.absolutePath)
        .onCompletion { onComplete(outFile) }
        .catch { throw RuntimeException("Failed to download $name", it) }
        .collect { println(it) }
}

suspend fun extractTarGz(
    inputPath: String,
    outputPath: String
) = process("tar", "-xzf", inputPath, "-C", outputPath) {
    onError {
        Log.e("ExtractProcess", it)
    }

    onOutput {
        Log.i("ExtractProcess", it)
    }
}.execute().success
