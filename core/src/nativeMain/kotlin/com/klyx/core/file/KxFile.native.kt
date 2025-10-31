package com.klyx.core.file

import com.klyx.core.io.MANAGE_ALL_FILES
import com.klyx.core.io.R_OK
import com.klyx.core.io.W_OK
import com.klyx.core.io.X_OK
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import platform.Foundation.NSURL
import platform.posix.F_OK
import platform.posix.access
import platform.posix.R_OK as POSIX_R_OK
import platform.posix.W_OK as POSIX_W_OK
import platform.posix.X_OK as POSIX_X_OK

actual open class KxFile(internal val nsurl: NSURL) {
    actual val name: String
        get() = TODO("Not yet implemented")
    actual val path: String
        get() = TODO("Not yet implemented")
    actual val absolutePath: String
        get() = TODO("Not yet implemented")
    actual val parent: String?
        get() = TODO("Not yet implemented")
    actual val parentFile: KxFile?
        get() = TODO("Not yet implemented")
    actual val exists: Boolean
        get() = TODO("Not yet implemented")
    actual val canRead: Boolean
        get() = TODO("Not yet implemented")
    actual val canWrite: Boolean
        get() = TODO("Not yet implemented")
    actual val canExecute: Boolean
        get() = TODO("Not yet implemented")
    actual val length: Long
        get() = TODO("Not yet implemented")
    actual val lastModified: Long
        get() = TODO("Not yet implemented")
    actual val extension: String
        get() = TODO("Not yet implemented")
    actual val isHidden: Boolean
        get() = TODO("Not yet implemented")
    actual val isFile: Boolean
        get() = TODO("Not yet implemented")
    actual val isDirectory: Boolean
        get() = TODO("Not yet implemented")

    actual fun mkdirs(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun mkdir(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun createNewFile(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun delete(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun deleteRecursively(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun renameTo(dest: KxFile): Boolean {
        TODO("Not yet implemented")
    }

    actual fun setReadable(readable: Boolean, ownerOnly: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    actual fun setWritable(writable: Boolean, ownerOnly: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    actual fun setExecutable(
        executable: Boolean,
        ownerOnly: Boolean
    ): Boolean {
        TODO("Not yet implemented")
    }

    actual fun list(): Array<String>? {
        TODO("Not yet implemented")
    }

    actual fun listFiles(): Array<KxFile>? {
        TODO("Not yet implemented")
    }

    actual fun listFiles(filter: (KxFile) -> Boolean): Array<KxFile>? {
        TODO("Not yet implemented")
    }

    actual fun readBytes(): ByteArray {
        TODO("Not yet implemented")
    }

    actual fun readText(charset: String): String {
        TODO("Not yet implemented")
    }

    actual fun writeBytes(bytes: ByteArray) {
    }

    actual fun writeText(text: String, charset: String) {
    }

    actual fun readLines(charset: String): List<String> {
        TODO("Not yet implemented")
    }

    actual override fun toString(): String {
        TODO("Not yet implemented")
    }
}

actual fun KxFile(path: String): KxFile {
    TODO("Not yet implemented")
}

actual fun KxFile(
    parent: KxFile,
    child: String
): KxFile {
    TODO("Not yet implemented")
}

actual fun KxFile(parent: String, child: String): KxFile {
    TODO("Not yet implemented")
}

actual fun KxFile(
    parent: KxFile,
    child: KxFile
): KxFile {
    TODO("Not yet implemented")
}

actual fun KxFile.isPermissionRequired(permissionFlags: Int): Boolean {
    var mode = F_OK

    if (permissionFlags and R_OK != 0) {
        mode = mode or POSIX_R_OK
    }

    if (permissionFlags and W_OK != 0) {
        mode = mode or POSIX_W_OK
    }

    if (permissionFlags and X_OK != 0) {
        mode = mode or POSIX_X_OK
    }

    if (permissionFlags and MANAGE_ALL_FILES != 0) {
        mode = mode or (POSIX_R_OK or POSIX_W_OK or POSIX_X_OK)
    }

    return access(absolutePath, mode) != 0
}

actual fun KxFile.source(): RawSource {
    TODO("Not yet implemented")
}

actual fun KxFile.sink(): RawSink {
    TODO("Not yet implemented")
}

actual fun KxFile.mimeType(): String? {
    TODO("Not yet implemented")
}
