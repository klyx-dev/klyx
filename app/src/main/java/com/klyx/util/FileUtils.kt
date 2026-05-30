package com.klyx.util

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the first 512 bytes of a file to determine if it is text file or binary file.
 */
suspend fun isTextFile(uri: Uri, contentResolver: ContentResolver) = withContext(Dispatchers.IO) {
    try {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val buffer = ByteArray(512)
            val bytesRead = inputStream.read(buffer)

            // An empty file is a perfectly valid text file
            if (bytesRead == -1) return@withContext true

            // Scan the read bytes for a NULL character (0x00)
            for (i in 0 until bytesRead) {
                if (buffer[i] == 0.toByte()) {
                    return@withContext false // Found NULL byte -> It's a binary file!
                }
            }

            true
        } ?: false
    } catch (_: Exception) {
        false // If we can't read it, treat it as unreadable/binary
    }
}

fun Long.humanBytes(): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = this.toDouble()
    var u = 0
    while (v >= 1024 && u < units.lastIndex) {
        v /= 1024.0
        u++
    }
    val rounded = when {
        v >= 100 -> "%.0f"
        v >= 10 -> "%.1f"
        else -> "%.2f"
    }
    return rounded.format(v) + " " + units[u]
}

fun Int.humanBytes(): String = toLong().humanBytes()
