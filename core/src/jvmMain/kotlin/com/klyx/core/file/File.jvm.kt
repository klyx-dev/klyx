package com.klyx.core.file

import com.klyx.core.PlatformContext
import java.awt.Desktop
import java.security.MessageDigest

actual fun KxFile.isBinaryEqualTo(other: KxFile): Boolean {
    if (!this.exists || !other.exists) return false
    if (this.length != other.length) return false

    this.inputStream().use { stream1 ->
        other.inputStream().use { stream2 ->
            val buffer1 = ByteArray(8192)
            val buffer2 = ByteArray(8192)

            while (true) {
                val read1 = stream1.read(buffer1)
                val read2 = stream2.read(buffer2)
                if (read1 != read2) return false
                if (read1 == -1) break
                if (!buffer1.contentEquals(buffer2)) return false
            }
        }
    }
    return true
}

actual fun KxFile.hash(algorithm: String): String {
    val digest = MessageDigest.getInstance(algorithm)
    this.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

/**
 * Launch system file opener
 */
actual fun openFile(file: KxFile) {
    Desktop.getDesktop().open(file.rawFile())
}

actual fun PlatformContext.shareFile(file: KxFile) {
    if (Desktop.isDesktopSupported()) openFile(file)
    else println("Desktop sharing not supported")
}
