package com.klyx.api.util

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Returns true if the file is likely a text file that can be opened in an editor.
 */
suspend fun isTextFile(
    uri: Uri,
    contentResolver: ContentResolver,
): Boolean = withContext(Dispatchers.IO) {
    try {
        val mimeType = contentResolver.getType(uri)

        // Known text types
        if (mimeType?.startsWith("text/") == true) {
            return@withContext true
        }

        // Known binary types
        if (
            mimeType == "application/pdf" ||
            mimeType == "application/zip" ||
            mimeType == "application/vnd.android.package-archive" ||
            mimeType?.startsWith("image/") == true ||
            mimeType?.startsWith("video/") == true ||
            mimeType?.startsWith("audio/") == true
        ) {
            return@withContext false
        }

        contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(4096)
            val bytesRead = input.read(buffer)

            // Empty files are valid text files.
            if (bytesRead <= 0) {
                return@withContext true
            }

            var suspicious = 0

            for (i in 0 until bytesRead) {
                val b = buffer[i].toInt() and 0xFF

                // NULL byte => almost certainly binary.
                if (b == 0) {
                    return@withContext false
                }

                val printable =
                    b == 0x09 || // tab
                            b == 0x0A || // newline
                            b == 0x0D || // carriage return
                            b in 0x20..0x7E

                if (!printable) {
                    suspicious++
                }
            }

            // More than 30% non-printable bytes => likely binary.
            suspicious.toFloat() / bytesRead < 0.30f
        } ?: false
    } catch (_: Exception) {
        false
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
