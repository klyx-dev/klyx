package com.klyx.core.file

import kotlinx.io.Source

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual open class KxFile {
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

    actual fun source(): Source {
        TODO("Not yet implemented")
    }

    actual override fun toString(): String {
        TODO("Not yet implemented")
    }
}