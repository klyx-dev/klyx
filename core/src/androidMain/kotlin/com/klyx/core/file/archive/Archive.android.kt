package com.klyx.core.file.archive

import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.util.zip.GZIPInputStream

actual suspend fun extractGzip(path: Path, destination: Path) {
    withContext(Dispatchers.IO) {
        SystemFileSystem.createDirectories(destination.parent ?: destination)

        SystemFileSystem.source(path).buffered().asInputStream().use { input ->
            GZIPInputStream(input).use { gz ->
                SystemFileSystem.sink(destination).buffered().asOutputStream().use { out ->
                    gz.copyTo(out)
                }
            }
        }
    }
}

actual suspend fun extractGzipTar(path: Path, destination: Path) {
    withContext(Dispatchers.IO) {
        val canonicalOutputDir = File(destination.toString()).canonicalFile

        SystemFileSystem.createDirectories(destination.parent ?: destination)

        SystemFileSystem.source(path).buffered().asInputStream().use { input ->
            GzipCompressorInputStream(input).use { gzIn ->
                TarArchiveInputStream(gzIn).use { tarIn ->
                    generateSequence { tarIn.nextEntry }.forEach { entry ->
                        val outputFile = File(destination.toString(), entry.name).canonicalFile

                        if (!outputFile.toPath().startsWith(canonicalOutputDir.toPath())) {
                            throw SecurityException("Zip Slip vulnerability detected! Malicious entry: ${entry.name}")
                        }

                        when {
                            entry.isDirectory -> {
                                outputFile.mkdirs()
                                applyMode(outputFile, entry.mode)
                            }

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

                                applyMode(outputFile, entry.mode)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun applyMode(file: File, mode: Int) {
    try {
        Os.chmod(file.absolutePath, mode)
    } catch (_: Exception) {
        val ownerExec = mode and 0b001_000_000 != 0
        val ownerRead = mode and 0b100_000_000 != 0
        val ownerWrite = mode and 0b010_000_000 != 0

        file.setExecutable(ownerExec, true)
        file.setReadable(ownerRead, true)
        file.setWritable(ownerWrite, true)
    }
}
