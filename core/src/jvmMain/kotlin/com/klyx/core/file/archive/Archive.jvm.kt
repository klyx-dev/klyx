package com.klyx.core.file.archive

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
import java.nio.file.Files
import java.nio.file.Paths
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
                            entry.isDirectory -> outputFile.mkdirs()

                            entry.isSymbolicLink -> {
                                try {
                                    Files.createSymbolicLink(Paths.get(entry.linkName), outputFile.toPath())
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
                    }
                }
            }
        }
    }
}
