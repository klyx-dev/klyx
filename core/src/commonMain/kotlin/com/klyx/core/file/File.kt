package com.klyx.core.file

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

typealias FileId = String

val KxFile.id: FileId
    get() = "$name:${length}:${lastModified}"

fun KxFile.isTextEqualTo(text: String): Boolean {
    if (!this.exists) return false
    return this.readText() == text
}

fun KxFile.isLinesEqualTo(lines: List<String>): Boolean {
    if (!this.exists) return false
    return this.readLines() == lines
}

fun KxFile.hasSameHashAs(hash: String, algorithm: String = "SHA-256"): Boolean {
    if (!this.exists) return false
    return this.hash(algorithm).equals(hash, ignoreCase = true)
}

fun KxFile.containsText(text: String): Boolean {
    if (!this.exists) return false
    return this.readText().contains(text)
}

fun KxFile.matchesRegex(regex: Regex): Boolean {
    if (!this.exists) return false
    return regex.containsMatchIn(this.readText())
}

expect fun KxFile.isBinaryEqualTo(other: KxFile): Boolean

fun KxFile.isTextuallyEqualTo(other: KxFile): Boolean {
    if (!this.exists || !other.exists) return false
    return this.readLines() == other.readLines()
}

expect fun KxFile.hash(algorithm: String = "SHA-256"): String

fun KxFile.isHashEqualTo(other: KxFile, algorithm: String = "SHA-256"): Boolean {
    if (!this.exists || !other.exists) return false
    return this.hash(algorithm) == other.hash(algorithm)
}

fun KxFile.isMetaEqualTo(other: KxFile): Boolean {
    return this.exists && other.exists &&
            this.name == other.name &&
            this.length == other.length &&
            this.lastModified == other.lastModified
}

/**
 * Launch system file opener
 */
expect fun openFile(file: KxFile)

expect fun ByteArray.isValidUtf8(): Boolean

suspend fun KxFile.isValidUtf8() = withContext(Dispatchers.IO) { readBytes().isValidUtf8() }
suspend fun PlatformFile.isValidUtf8() = readBytes().isValidUtf8()
