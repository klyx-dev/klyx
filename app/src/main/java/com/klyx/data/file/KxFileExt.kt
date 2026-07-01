package com.klyx.data.file

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.klyx.api.data.file.FileStatInfo
import com.klyx.api.data.file.KxFile
import com.klyx.native.Os as NativeOs
import com.klyx.api.util.applicationContext
import com.klyx.api.util.tryOrNull
import com.klyx.api.util.withApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

data class SizeProgress(
    val bytes: Long,
    val fileCount: Int,
    val dirCount: Int,
    val isFinished: Boolean
)

fun KxFile.calculateTotalSize() = flow {
    // If it's a file, emit 1 file, 0 dirs, and finish immediately
    if (!isDirectory) {
        emit(SizeProgress(length, fileCount = 1, dirCount = 0, isFinished = true))
        return@flow
    }

    var totalSize = 0L
    var fileCount = 0
    var dirCount = 0

    val directories = ArrayDeque<KxFile>()
    directories.add(this@calculateTotalSize)

    while (directories.isNotEmpty()) {
        if (!currentCoroutineContext().isActive) break

        val currentDir = directories.removeFirst()
        val children = currentDir.listFiles()

        for (child in children) {
            if (child.isDirectory) {
                dirCount++
                directories.add(child)
            } else {
                fileCount++
                totalSize += child.length
            }

            emit(SizeProgress(totalSize, fileCount, dirCount, isFinished = false))
        }
    }

    emit(SizeProgress(totalSize, fileCount, dirCount, isFinished = true))
}.flowOn(Dispatchers.IO)

val KxFile.stat: StructStat?
    get() = tryOrNull {
        if (file != null) {
            Os.stat(file!!.absolutePath)
        } else {
            applicationContext().contentResolver.openFileDescriptor(uri, "r")?.use {
                Os.fstat(it.fileDescriptor)
            }
        }
    }

val KxFile.permissionsString: String
    get() {
        val mode = if (file != null) {
            try {
                Os.stat(file!!.absolutePath).st_mode
            } catch (_: ErrnoException) {
                null
            }
        } else if (!isDirectory) {
            try {
                applicationContext().contentResolver
                    .openFileDescriptor(uri, "r")
                    ?.use { Os.fstat(it.fileDescriptor).st_mode }
            } catch (_: Exception) {
                null
            }
        } else null

        if (mode != null) {
            return buildString {
                append(if (OsConstants.S_ISDIR(mode)) "d" else "-")

                append(if (mode and OsConstants.S_IRUSR != 0) "r" else "-")
                append(if (mode and OsConstants.S_IWUSR != 0) "w" else "-")
                append(if (mode and OsConstants.S_IXUSR != 0) "x" else "-")

                append(if (mode and OsConstants.S_IRGRP != 0) "r" else "-")
                append(if (mode and OsConstants.S_IWGRP != 0) "w" else "-")
                append(if (mode and OsConstants.S_IXGRP != 0) "x" else "-")

                append(if (mode and OsConstants.S_IROTH != 0) "r" else "-")
                append(if (mode and OsConstants.S_IWOTH != 0) "w" else "-")
                append(if (mode and OsConstants.S_IXOTH != 0) "x" else "-")
            }
        }

        val grant = applicationContext().contentResolver
            .persistedUriPermissions
            .find { it.uri == uri }
        val r = if (grant?.isReadPermission ?: raw.canRead()) "r" else "-"
        val w = if (grant?.isWritePermission ?: raw.canWrite()) "w" else "-"
        val x = if (isDirectory && r == "r") "x" else "-"
        return "${if (isDirectory) "d" else "-"}$r$w$x------"
    }

val KxFile.isSymlink: Boolean
    get() = try {
        if (file != null) {
            val mode = Os.lstat(file!!.absolutePath).st_mode
            OsConstants.S_ISLNK(mode)
        } else false // SAF URIs can't be symlinks from our perspective
    } catch (_: Exception) {
        false
    }

val KxFile.symlinkTarget
    get() = tryOrNull {
        if (file != null) {
            val mode = Os.lstat(file!!.absolutePath).st_mode
            if (OsConstants.S_ISLNK(mode)) Os.readlink(file!!.absolutePath) else null
        } else null
    }

val KxFile.isProtectedPath: Boolean
    @SuppressLint("SdCardPath")
    get() {
        val path = file?.canonicalPath ?: absolutePath
        return path == Environment.getExternalStorageDirectory().canonicalPath
                || path == "/sdcard"
                || path == "/storage/self/primary"
                || path == applicationContext().dataDir.canonicalPath
                || path == applicationContext().filesDir.canonicalPath
                || path.startsWith("/proc")
                || path.startsWith("/sys")
                || path.startsWith("/dev")
    }

val KxFile.shareableUri: Uri
    get() = if (!isSafDocument) {
        val context = applicationContext()
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file!!)
    } else uri

fun KxFile.mimeType() = when (extension.lowercase()) {
    "xml" -> "text/xml"
    "json" -> "application/json"
    "md" -> "text/markdown"
    else -> MimeTypeMap
        .getSingleton()
        .getMimeTypeFromExtension(extension.lowercase())
}

fun KxFile.openWith() = withApplicationContext {
    if (isDirectory) return@withApplicationContext

    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(shareableUri, mimeType() ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(
            Intent.createChooser(intent, "Open with")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            applicationContext,
            "No application found to open this file",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        Toast.makeText(
            applicationContext,
            "Could not open file: ${e.localizedMessage}",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun KxFile.share() = withApplicationContext {
    if (isDirectory) return@withApplicationContext

    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType() ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, shareableUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(
            Intent
                .createChooser(intent, "Share file")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            applicationContext,
            "No application available for sharing",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        Toast.makeText(
            applicationContext,
            "Could not share file: ${e.localizedMessage}",
            Toast.LENGTH_LONG
        ).show()
    }
}

val KxFile.statInfo: FileStatInfo?
    get() {
        val stat = try {
            if (file != null) {
                Os.stat(file!!.absolutePath)
            } else if (!isDirectory) {
                applicationContext().contentResolver
                    .openFileDescriptor(uri, "r")
                    ?.use { Os.fstat(it.fileDescriptor) }
            } else null
        } catch (_: Exception) {
            null
        } ?: return null

        val ownerName = try {
            NativeOs.getpwuid(stat.st_uid)?.pw_name ?: stat.st_uid.toString()
        } catch (_: Exception) {
            stat.st_uid.toString()
        }

        val groupName = try {
            NativeOs.getgrgid(stat.st_gid)?.gr_name ?: stat.st_gid.toString()
        } catch (_: Exception) {
            stat.st_gid.toString()
        }

        return FileStatInfo(
            mode = stat.st_mode,
            permissions = permissionsString,
            ownerUid = stat.st_uid,
            ownerName = ownerName,
            groupGid = stat.st_gid,
            groupName = groupName,
            hardLinks = stat.st_nlink,
            inode = stat.st_ino,
            deviceId = stat.st_dev,
            blockSize = stat.st_blksize,
            blocksAllocated = stat.st_blocks,
            lastAccessed = stat.st_atim.tv_sec,
            lastModified = stat.st_mtim.tv_sec,
            lastChanged = stat.st_ctim.tv_sec,
        )
    }
