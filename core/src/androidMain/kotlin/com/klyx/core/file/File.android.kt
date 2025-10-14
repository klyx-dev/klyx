package com.klyx.core.file

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.klyx.core.ContextHolder
import com.klyx.core.PlatformContext
import com.klyx.core.io.MANAGE_ALL_FILES
import com.klyx.core.io.R_OK
import com.klyx.core.io.W_OK
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

fun KxFile.requiresPermission(context: Context, flags: Int): Boolean {
    // External storage root check
    val externalDirs = context.getExternalFilesDirs(null).mapNotNull { it?.parentFile?.parentFile?.parentFile }
    val isExternalStorage = externalDirs.any { this.absolutePath.startsWith(it.absolutePath) }

    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            if (flags and MANAGE_ALL_FILES != 0 || ((flags and R_OK) != 0 && (flags and W_OK) != 0)) {
                !Environment.isExternalStorageManager()
            } else {
                !this.isInAppSpecificDir(context) && !this.canAccess()
            }
        }

        isExternalStorage -> {
            // If ANY required permission is missing, return true
            val permissionsToCheck = mutableListOf<String>()

            if (flags and R_OK != 0) {
                permissionsToCheck += Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (flags and W_OK != 0) {
                permissionsToCheck += Manifest.permission.WRITE_EXTERNAL_STORAGE
            }

            permissionsToCheck.any {
                context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }
        }

        else -> false // Internal storage or app-private dirs
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

internal fun KxFile.canAccess(): Boolean {
    return if (exists) {
        if (canRead && canWrite) return true
        false
    } else {
        val parent = this.parentFile ?: return false
        parent.canWrite
    }
}

/**
 * Launch system file opener
 */
actual fun openFile(file: KxFile) {
    val context = ContextHolder.context
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file.rawFile()
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "text/plain")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(intent, "Open with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

actual fun PlatformContext.shareFile(file: KxFile) {
    val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file.rawFile())

    val intent = Intent(Intent.ACTION_SEND).apply {
        setDataAndType(uri, "text/plain")
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    startActivity(
        Intent.createChooser(intent, "Share file via...").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
