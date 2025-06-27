package com.klyx.core.file

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

actual fun KxFile.isBinaryEqualTo(other: KxFile): Boolean {
    if (!this.exists || !other.exists) return false
    if (this.length != other.length) return false

    this.inputStream().use { stream1 ->
        other.inputStream().use { stream2 ->
            val buffer1 = ByteArray(8192)
            val buffer2 = ByteArray(8192)

            while (true) {
                val read1 = stream1?.read(buffer1)
                val read2 = stream2?.read(buffer2)
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
    this.inputStream()?.use { input ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun KxFile.requiresPermission(context: Context, isWrite: Boolean): Boolean {
    // External storage root check
    val externalDirs = context.getExternalFilesDirs(null).mapNotNull { it?.parentFile?.parentFile?.parentFile }
    val isExternalStorage = externalDirs.any { this.absolutePath.startsWith(it.absolutePath) }

    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            // For Android 11 and above (Scoped Storage)
            // Apps can access their own app-specific dirs freely
            !this.isInAppSpecificDir(context) && !this.canAccess()
        }

        isExternalStorage -> {
            val permission = if (isWrite) Manifest.permission.WRITE_EXTERNAL_STORAGE else Manifest.permission.READ_EXTERNAL_STORAGE
            context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }

        else -> false // Internal storage or app-private paths generally don't require extra permission
    }
}

private fun KxFile.isInAppSpecificDir(context: Context): Boolean {
    val appSpecificDirs = listOfNotNull(
        context.filesDir,
        context.cacheDir,
        context.externalCacheDir,
        context.getExternalFilesDir(null)
    ).map { it.absolutePath }

    return appSpecificDirs.any { this.absolutePath.startsWith(it) }
}

private fun KxFile.canAccess(): Boolean {
    return if (exists) {
        if (canRead && canWrite) return true
        false
    } else {
        val parent = this.parentFile ?: return false
        parent.canWrite
    }
}

