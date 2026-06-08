package com.klyx.data.file.archive

import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

private suspend fun extractGzipFromInputStream(input: InputStream, destination: File) {
    withContext(Dispatchers.IO) {
        GZIPInputStream(input).use { gz ->
            destination.outputStream().use { out ->
                gz.copyTo(out)
            }
        }
    }
}

private suspend fun extractGzipTarFromInputStream(input: InputStream, destination: File) {
    withContext(Dispatchers.IO) {
        val canonicalOutputDir = destination.canonicalFile

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

suspend fun extractGzip(path: File, destination: File) {
    withContext(Dispatchers.IO) {
        destination.parentFile?.mkdirs() ?: destination.mkdirs()
        path.inputStream().use { inputStream ->
            extractGzipFromInputStream(inputStream, destination)
        }
    }
}

suspend fun extractGzipTar(path: File, destination: File) {
    withContext(Dispatchers.IO) {
        destination.parentFile?.mkdirs() ?: destination.mkdirs()
        path.inputStream().use { input ->
            extractGzipTarFromInputStream(input, destination)
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

                if (!canonicalOut.toString().startsWith(canonicalDest.toString())) {
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
