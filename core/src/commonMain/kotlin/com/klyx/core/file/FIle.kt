package com.klyx.core.file

import java.io.File
import java.security.MessageDigest

typealias FileId = String

val File.id: FileId
    get() = "$name:${length()}:${lastModified()}"

fun File.isTextEqualTo(text: String): Boolean {
    if (!this.exists()) return false
    return this.readText() == text
}

fun File.isLinesEqualTo(lines: List<String>): Boolean {
    if (!this.exists()) return false
    return this.readLines() == lines
}

fun File.hasSameHashAs(hash: String, algorithm: String = "SHA-256"): Boolean {
    if (!this.exists()) return false
    return this.hash(algorithm).equals(hash, ignoreCase = true)
}

fun File.containsText(text: String): Boolean {
    if (!this.exists()) return false
    return this.readText().contains(text)
}

fun File.matchesRegex(regex: Regex): Boolean {
    if (!this.exists()) return false
    return regex.containsMatchIn(this.readText())
}

fun File.isBinaryEqualTo(other: File): Boolean {
    if (!this.exists() || !other.exists()) return false
    if (this.length() != other.length()) return false

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

fun File.isTextuallyEqualTo(other: File): Boolean {
    if (!this.exists() || !other.exists()) return false
    return this.readLines() == other.readLines()
}

fun File.hash(algorithm: String = "SHA-256"): String {
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

fun File.isHashEqualTo(other: File, algorithm: String = "SHA-256"): Boolean {
    if (!this.exists() || !other.exists()) return false
    return this.hash(algorithm) == other.hash(algorithm)
}

fun File.isMetaEqualTo(other: File): Boolean {
    return this.exists() && other.exists() &&
            this.name == other.name &&
            this.length() == other.length() &&
            this.lastModified() == other.lastModified()
}
