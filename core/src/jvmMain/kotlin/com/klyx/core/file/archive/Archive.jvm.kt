package com.klyx.core.file.archive

import com.klyx.core.file.toOkioPath
import com.klyx.core.io.fs
import com.klyx.core.io.okioFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import kotlinx.io.buffered
import kotlinx.io.files.Path
import okio.buffer
import okio.source
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

private suspend fun extractGzipFromInputStream(input: InputStream, destination: Path) {
    withContext(Dispatchers.IO) {
        GZIPInputStream(input).use { gz ->
            fs.sink(destination).buffered().asOutputStream().use { out ->
                gz.copyTo(out)
            }
        }
    }
}

actual suspend fun extractGzip(path: Path, destination: Path) {
    withContext(Dispatchers.IO) {
        fs.createDirectories(destination.parent ?: destination)
        fs.source(path).buffered().asInputStream().use { inputStream ->
            extractGzipFromInputStream(inputStream, destination)
        }
    }
}

private suspend fun extractGzipTarFromInputStream(input: InputStream, destination: Path) {
    withContext(Dispatchers.IO) {
        val canonicalOutputDir = File(destination.toString()).canonicalFile

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
                            chmod(outputFile, entry.mode)
                        }

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
                            chmod(outputFile, entry.mode)
                        }
                    }
                }
            }
        }
    }
}

actual suspend fun extractGzipTar(path: Path, destination: Path) {
    withContext(Dispatchers.IO) {
        fs.createDirectories(destination.parent ?: destination)
        fs.source(path).buffered().asInputStream().use { input ->
            extractGzipTarFromInputStream(input, destination)
        }
    }
}

private fun chmod(file: File, mode: Int) {
    val perms = mutableSetOf<PosixFilePermission>()

    if (mode and 0b100_000_000 != 0) perms += PosixFilePermission.OWNER_READ
    if (mode and 0b010_000_000 != 0) perms += PosixFilePermission.OWNER_WRITE
    if (mode and 0b001_000_000 != 0) perms += PosixFilePermission.OWNER_EXECUTE

    if (mode and 0b000_100_000 != 0) perms += PosixFilePermission.GROUP_READ
    if (mode and 0b000_010_000 != 0) perms += PosixFilePermission.GROUP_WRITE
    if (mode and 0b000_001_000 != 0) perms += PosixFilePermission.GROUP_EXECUTE

    if (mode and 0b000_000_100 != 0) perms += PosixFilePermission.OTHERS_READ
    if (mode and 0b000_000_010 != 0) perms += PosixFilePermission.OTHERS_WRITE
    if (mode and 0b000_000_001 != 0) perms += PosixFilePermission.OTHERS_EXECUTE

    Files.setPosixFilePermissions(file.toPath(), perms)
}

actual suspend fun extractGzip(bytes: ByteArray, destination: Path) {
    extractGzipFromInputStream(bytes.inputStream(), destination)
}

actual suspend fun extractGzipTar(bytes: ByteArray, destination: Path) {
    extractGzipTarFromInputStream(bytes.inputStream(), destination)
}

actual suspend fun extractZip(bytes: ByteArray, destination: Path) {
    val destination = destination.toOkioPath()

    withContext(Dispatchers.IO) {
        okioFs.createDirectories(destination)

        ZipInputStream(bytes.inputStream()).use { zipIn ->
            generateSequence { zipIn.nextEntry }.forEach { entry ->
                val outPath = destination.resolve(entry.name)

                val canonicalDest = okioFs.canonicalize(destination)
                val canonicalOut = okioFs.canonicalize(outPath)

                if (!canonicalOut.toString().startsWith(canonicalDest.toString())) {
                    throw SecurityException("Zip Slip vulnerability detected! Malicious entry: ${entry.name}")
                }

                if (entry.isDirectory) {
                    okioFs.createDirectories(outPath)
                } else {
                    okioFs.createDirectories(outPath.parent!!)
                    okioFs.sink(outPath).buffer().use { sink ->
                        sink.writeAll(zipIn.source())
                    }
                }

                zipIn.closeEntry()
            }
        }
    }
}
