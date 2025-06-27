package com.klyx.core.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

actual suspend fun unzipFile(zipPath: String, destination: String) = withContext(Dispatchers.IO) {
    val zipInput = ZipInputStream(File(zipPath).inputStream())
    var entry = zipInput.nextEntry
    while (entry != null) {
        val outFile = File(destination, entry.name)
        if (entry.isDirectory) {
            outFile.mkdirs()
        } else {
            outFile.parentFile.mkdirs()
            outFile.outputStream().use { output ->
                zipInput.copyTo(output)
            }
        }
        zipInput.closeEntry()
        entry = zipInput.nextEntry
    }
    zipInput.close()
}
