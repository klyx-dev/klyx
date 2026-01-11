package com.klyx.core.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import okio.SYSTEM
import okio.buffer
import okio.use

val fs by lazy { SystemFileSystem }
val okioFs by lazy { okio.FileSystem.SYSTEM }

suspend fun okio.FileSystem.copyRecursively(source: okio.Path, target: okio.Path, overwrite: Boolean = true) {
    withContext(Dispatchers.IO) {
        val metadata = metadata(source)
        if (metadata.isDirectory) {
            createDirectories(target)

            for (child in list(source)) {
                val childTarget = target / child.name
                copyRecursively(child, childTarget, overwrite)
            }
        } else {
            if (!overwrite && exists(target)) return@withContext
            source(source).use { input ->
                sink(target).buffer().use { output ->
                    output.writeAll(input)
                }
            }
        }
    }
}

suspend fun okio.FileSystem.isDirectory(path: okio.Path) = withContext(Dispatchers.IO) {
    metadataOrNull(path)?.isDirectory ?: false
}

suspend fun okio.FileSystem.isFile(path: okio.Path) = withContext(Dispatchers.IO) {
    metadataOrNull(path)?.isRegularFile ?: false
}

suspend fun FileSystem.isFile(path: Path) = withContext(Dispatchers.IO) {
    metadataOrNull(path)?.isRegularFile ?: false
}
