package com.klyx.core.file

import android.Manifest
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
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

/**
 * Watches directory. If file is supplied it will use parent directory. If it's an intent to watch just file,
 * developers must filter for the file related events themselves.
 *
 * From: https://github.com/vishna/watchservice-ktx
 *
 * @param [mode] - mode in which we should observe changes, can be SingleFile, SingleDirectory, Recursive
 * @param [tag] - any kind of data that should be associated with this channel
 * @param [scope] - coroutine context for the channel, optional
 */
@OptIn(DelicateCoroutinesApi::class)
fun File.asWatchChannel(
    mode: KWatchChannel.Mode? = null,
    tag: Any? = null,
    scope: CoroutineScope = GlobalScope
) = KWatchChannel(
    file = this,
    mode = mode ?: if (isFile) KWatchChannel.Mode.SingleFile else KWatchChannel.Mode.Recursive,
    scope = scope,
    tag = tag
)

fun FileWrapper.requiresPermission(context: Context, isWrite: Boolean): Boolean {
    // External storage root check
    val externalDirs = context.getExternalFilesDirs(null).mapNotNull { it?.parentFile?.parentFile?.parentFile }
    val isExternalStorage = externalDirs.any { this.absolutePath.startsWith(it.absolutePath) }

    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            // For Android 11 and above (Scoped Storage)
            // Apps can access their own app-specific dirs freely
            !this.isInAppSpecificDir(context) && !this.canAccess(context)
        }

        isExternalStorage -> {
            val permission = if (isWrite) Manifest.permission.WRITE_EXTERNAL_STORAGE else Manifest.permission.READ_EXTERNAL_STORAGE
            context.checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        else -> false // Internal storage or app-private paths generally don't require extra permission
    }
}

private fun FileWrapper.isInAppSpecificDir(context: Context): Boolean {
    val appSpecificDirs = listOfNotNull(
        context.filesDir,
        context.cacheDir,
        context.externalCacheDir,
        context.getExternalFilesDir(null)
    ).map { it.absolutePath }

    return appSpecificDirs.any { this.absolutePath.startsWith(it) }
}

private fun FileWrapper.canAccess(context: Context): Boolean {
    return if (exists()) {
        if (canRead() && canWrite()) true
        else try {
            if (isDirectory) list()?.isNotEmpty() != null
            else inputStream(context)?.close() != null
        } catch (e: Exception) {
            false
        }
    } else {
        val parent = this.parentFile ?: return false
        parent.canWrite()
    }
}
