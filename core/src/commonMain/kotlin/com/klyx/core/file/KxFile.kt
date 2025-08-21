package com.klyx.core.file

import com.klyx.fileSeparatorChar
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import kotlinx.io.Source

/**
 * Represents a file or directory in the file system.
 */
expect open class KxFile {
    val name: String
    val path: String
    val absolutePath: String
    val parent: String?
    val parentFile: KxFile?
    val exists: Boolean
    val canRead: Boolean
    val canWrite: Boolean
    val canExecute: Boolean
    val length: Long
    val lastModified: Long
    val extension: String
    val isHidden: Boolean
    val isFile: Boolean
    val isDirectory: Boolean

    fun mkdirs(): Boolean
    fun mkdir(): Boolean
    fun createNewFile(): Boolean
    fun delete(): Boolean
    fun deleteRecursively(): Boolean
    fun renameTo(dest: KxFile): Boolean
    fun setReadable(readable: Boolean, ownerOnly: Boolean = true): Boolean
    fun setWritable(writable: Boolean, ownerOnly: Boolean = true): Boolean
    fun setExecutable(executable: Boolean, ownerOnly: Boolean = true): Boolean

    fun list(): Array<String>?
    fun listFiles(): Array<KxFile>?
    fun listFiles(filter: (KxFile) -> Boolean): Array<KxFile>?

    fun readBytes(): ByteArray
    fun readText(charset: String = "UTF-8"): String
    fun writeBytes(bytes: ByteArray)
    fun writeText(text: String, charset: String = "UTF-8")
    fun readLines(charset: String = "UTF-8"): List<String>

    fun source(): Source

    override fun toString(): String
}

expect fun KxFile(path: String): KxFile
expect fun KxFile(parent: KxFile, child: String): KxFile
expect fun KxFile(parent: String, child: String): KxFile
expect fun KxFile(parent: KxFile, child: KxFile): KxFile

fun KxFile.find(name: String): KxFile? = listFiles()?.find { it.name == name }

fun KxFile.resolve(relative: KxFile): KxFile {
    val baseName = this.toString()
    return if (baseName.isEmpty() || baseName.endsWith(fileSeparatorChar)) {
        KxFile(baseName + relative)
    } else {
        KxFile(baseName + fileSeparatorChar + relative)
    }
}

fun KxFile.resolve(relative: String): KxFile = resolve(KxFile(relative))

fun PlatformFile.toKxFile() = KxFile(absolutePath())
