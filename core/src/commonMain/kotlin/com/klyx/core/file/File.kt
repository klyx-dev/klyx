package com.klyx.core.file

import com.klyx.core.format
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.openZip
import okio.use

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

suspend fun unzip(zipPath: Path, destDir: Path) = withContext(Dispatchers.IO) {
    val fs = FileSystem.SYSTEM
    fs.createDirectories(destDir)

    fs.openZip(zipPath).use { zipFs ->
        for (entry in zipFs.listRecursively("/".toPath())) {
            val target = destDir / entry.toString().removePrefix("/")
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
