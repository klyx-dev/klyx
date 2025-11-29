package com.klyx.core.file.archive

import com.klyx.core.file.toOkioPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.openZip
import okio.use

/**
 * Extract `.gz`
 */
expect suspend fun extractGzip(path: Path, destination: Path)

/**
 * Extract `.tar.gz`
 */
expect suspend fun extractGzipTar(path: Path, destination: Path)

/**
 * Extract `.zip`
 */
suspend fun extractZip(path: Path, destination: Path) = withContext(Dispatchers.IO) {
    val fs = FileSystem.SYSTEM

    fs.openZip(path.toOkioPath()).use { zipFs ->
        for (entry in zipFs.listRecursively("/".toPath())) {
            val target = destination.toOkioPath() / entry.toString().removePrefix("/")
            val metadata = zipFs.metadataOrNull(entry)

            if (metadata?.isDirectory == true) {
                fs.createDirectories(target)
            } else {
                fs.createDirectories(target.parent!!)
                zipFs.source(entry).use { input ->
                    fs.sink(target).use { output ->
                        input.buffer().readAll(output)
                    }
                }
            }
        }
    }
}

/**
 * Extract `.gz`
 */
expect suspend fun extractGzip(bytes: ByteArray, destination: Path)

/**
 * Extract `.tar.gz`
 */
expect suspend fun extractGzipTar(bytes: ByteArray, destination: Path)

/**
 * Extract `.zip`
 */
expect suspend fun extractZip(bytes: ByteArray, destination: Path)
