package com.klyx.core.extension

import com.klyx.core.file.KxFile
import com.klyx.core.file.outputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

internal actual suspend fun ByteArray.extractRepoZip(targetDir: KxFile) = withContext(Dispatchers.IO) {
    ZipInputStream(inputStream()).use { input ->
        var entry = input.nextEntry

        while (entry != null) {
            val name = entry.name.substringAfter("/")

            if (name.isEmpty()) {
                entry = input.nextEntry
                continue
            }

            val outputFile = KxFile(targetDir.absolutePath, name)

            if (entry.isDirectory) {
                outputFile.mkdirs()
            } else {
                outputFile.parentFile?.mkdirs()
                outputFile.outputStream().use(input::copyTo)
            }

            entry = input.nextEntry
        }
    }
}
