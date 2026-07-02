package com.klyx.data.file.archive

import android.system.Os
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream


typealias ProgressListener = (ExtractionProgress) -> Unit

data class ExtractionProgress(
    val currentFile: String,
    val extractedBytes: Long,
    val totalBytes: Long
) {
    val percent: Int get() = if (totalBytes > 0) ((extractedBytes * 100) / totalBytes).toInt() else 0
}

private class ProgressInputStream(
    private val input: InputStream,
    private val totalBytes: Long,
    private val listener: ProgressListener?
) : InputStream() {

    @Volatile
    var currentFile: String = ""

    private var extractedBytes = 0L
    private var lastPercent = -1

    private fun report(count: Int) {
        if (count <= 0 || listener == null) return

        extractedBytes += count
        val percent = if (totalBytes > 0) ((extractedBytes * 100) / totalBytes).toInt() else -1

        if (percent != lastPercent) {
            lastPercent = percent

            listener(
                ExtractionProgress(
                    currentFile = currentFile,
                    extractedBytes = extractedBytes,
                    totalBytes = totalBytes
                )
            )
        }
    }

    override fun read(): Int {
        val result = input.read()

        if (result != -1) {
            report(1)
        }

        return result
    }

    override fun read(
        b: ByteArray,
        off: Int,
        len: Int
    ): Int {
        val count = input.read(b, off, len)

        if (count > 0) {
            report(count)
        }

        return count
    }

    override fun close() {
        input.close()
    }
}

private fun extractGzipFromInputStream(input: InputStream, destination: File) {
    GZIPInputStream(input).use { gz ->
        destination.outputStream().use { out ->
            gz.copyTo(out)
        }
    }
}

private fun extractGzipTarFromInputStream(input: InputStream, destination: File) {
    val canonicalOutputDir = destination.canonicalFile

    GzipCompressorInputStream(input).use { gzIn ->
        TarArchiveInputStream(gzIn).use { tarIn ->
            generateSequence { tarIn.nextEntry }.forEach { entry ->
                val outputFile = File(destination, entry.name).canonicalFile

                if (!outputFile.toPath().startsWith(canonicalOutputDir.toPath())) {
                    throw SecurityException("Zip Slip vulnerability detected! Malicious entry: ${entry.name}")
                }

                when {
                    entry.isDirectory -> {
                        outputFile.mkdirs()
                        applyMode(outputFile, entry.mode)
                    }

                    entry.isSymbolicLink -> {
                        extractSymlink(
                            destination,
                            outputFile,
                            entry.linkName
                        )
                    }

                    entry.isLink -> {
                        extractHardLink(
                            destination,
                            outputFile,
                            entry.linkName
                        )
                    }

                    else -> {
                        outputFile.parentFile?.mkdirs()
                        outputFile.outputStream().use { fos ->
                            tarIn.copyTo(fos)
                        }

                        applyMode(outputFile, entry.mode)
                    }
                }
            }
        }
    }
}

private fun extractXzTarFromInputStream(input: ProgressInputStream, destination: File) {
    val rootDir = destination.canonicalFile

    XZCompressorInputStream(input).use { xzIn ->
        TarArchiveInputStream(xzIn).use { tarIn ->
            while (true) {
                val entry = tarIn.nextEntry ?: break
                input.currentFile = entry.name

                val outputFile = File(destination, entry.name).canonicalFile
                if (!outputFile.toPath().startsWith(rootDir.toPath())) {
                    throw SecurityException("Tar traversal detected: ${entry.name}")
                }

                when {
                    entry.isDirectory -> {
                        outputFile.mkdirs()
                        applyMode(outputFile, entry.mode)
                    }

                    entry.isSymbolicLink -> {
                        extractSymlink(
                            destination,
                            outputFile,
                            entry.linkName
                        )
                    }

                    entry.isLink -> {
                        extractHardLink(
                            destination,
                            outputFile,
                            entry.linkName
                        )
                    }

                    else -> {
                        outputFile.parentFile?.mkdirs()

                        outputFile.outputStream()
                            .buffered()
                            .use { output ->
                                tarIn.copyTo(output)
                            }

                        applyMode(outputFile, entry.mode)
                    }
                }
            }
        }
    }
}

suspend fun extractXzTar(archive: File, destination: File, listener: ProgressListener? = null) =
    withContext(Dispatchers.IO) {
        destination.mkdirs()

        val progressInput = ProgressInputStream(
            input = archive.inputStream().buffered(),
            totalBytes = archive.length(),
            listener = listener
        )

        progressInput.use {
            extractXzTarFromInputStream(
                it,
                destination
            )
        }
    }

private fun extractSymlink(destination: File, linkFile: File, target: String) {
    linkFile.parentFile?.mkdirs()

    try {
        val targetFile = File(linkFile.parentFile, target).canonicalFile

        if (!targetFile.toPath().startsWith(destination.canonicalFile.toPath())) {
            throw SecurityException("Symlink escapes destination: $target")
        }

        Os.symlink(target, linkFile.absolutePath)
    } catch (e: Exception) {
        throw IOException("Failed to create symlink ${linkFile.path} -> $target", e)
    }
}

private fun extractHardLink(destination: File, linkFile: File, target: String) {
    linkFile.parentFile?.mkdirs()

    val targetFile = File(destination, target).canonicalFile
    if (!targetFile.toPath().startsWith(destination.canonicalFile.toPath())) {
        throw SecurityException("Hardlink escapes destination: $target")
    }

    try {
        Files.createLink(
            linkFile.toPath(),
            targetFile.toPath()
        )
    } catch (_: Exception) {
        Files.copy(
            targetFile.toPath(),
            linkFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }
}

private fun applyMode(file: File, mode: Int) {
    try {
        Os.chmod(file.absolutePath, mode)
    } catch (t: Throwable) {
        // chmod can fail on some filesystems
        Log.w("extract", "chmod(${file.absolutePath}, ${Integer.toOctalString(mode)}) failed, using File.setX fallback", t)

        file.setReadable(
            mode and 0b100_000_000 != 0,
            true
        )

        file.setWritable(
            mode and 0b010_000_000 != 0,
            true
        )

        file.setExecutable(
            mode and 0b001_000_000 != 0,
            true
        )
    }
}

suspend fun extractGzip(path: File, destination: File) {
    withContext(Dispatchers.IO) {
        destination.mkdirs()
        path.inputStream().buffered().use { inputStream ->
            extractGzipFromInputStream(inputStream, destination)
        }
    }
}

suspend fun extractGzipTar(path: File, destination: File) {
    withContext(Dispatchers.IO) {
        destination.mkdirs()
        path.inputStream().buffered().use { input ->
            extractGzipTarFromInputStream(input, destination)
        }
    }
}

suspend fun extractGzip(bytes: ByteArray, destination: File) {
    extractGzipFromInputStream(bytes.inputStream(), destination)
}

suspend fun extractGzipTar(bytes: ByteArray, destination: File) {
    extractGzipTarFromInputStream(bytes.inputStream(), destination)
}

suspend fun extractZip(bytes: ByteArray, destination: File) {
    withContext(Dispatchers.IO) {
        destination.mkdirs()

        ZipInputStream(bytes.inputStream()).use { zipIn ->
            generateSequence { zipIn.nextEntry }.forEach { entry ->
                val outPath = destination.resolve(entry.name)

                val canonicalDest = destination.canonicalFile
                val canonicalOut = outPath.canonicalFile

                if (!canonicalOut.toPath().startsWith(canonicalDest.toPath())) {
                    throw SecurityException("Zip Slip vulnerability detected! Malicious entry: ${entry.name}")
                }

                if (entry.isDirectory) {
                    outPath.mkdirs()
                } else {
                    outPath.parentFile!!.mkdirs()
                    outPath.outputStream().buffered().use { out ->
                        zipIn.copyTo(out)
                    }
                }

                zipIn.closeEntry()
            }
        }
    }
}
