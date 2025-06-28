package com.klyx.core.file

import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual open class KxFile(
    private val raw: File
) {
    actual val name: String get() = raw.name
    actual val path: String get() = raw.path
    actual val absolutePath: String get() = raw.absolutePath
    actual val parent: String? get() = raw.parent
    actual val parentFile: KxFile? get() = raw.parentFile?.let(::KxFile)
    actual val exists: Boolean get() = raw.exists()
    actual val canRead: Boolean get() = raw.canRead()
    actual val canWrite: Boolean get() = raw.canWrite()
    actual val canExecute: Boolean get() = raw.canExecute()
    actual val length: Long get() = raw.length()
    actual val lastModified: Long get() = raw.lastModified()
    actual val extension: String get() = raw.extension
    actual val isHidden: Boolean get() = raw.isHidden
    actual val isFile: Boolean get() = raw.isFile
    actual val isDirectory: Boolean get() = raw.isDirectory

    actual fun mkdirs(): Boolean = raw.mkdirs()
    actual fun mkdir(): Boolean = raw.mkdir()
    actual fun createNewFile(): Boolean = raw.createNewFile()
    actual fun delete(): Boolean = raw.delete()
    actual fun deleteRecursively(): Boolean = raw.deleteRecursively()
    actual fun renameTo(dest: KxFile): Boolean = raw.renameTo(dest.raw)

    actual fun setReadable(readable: Boolean, ownerOnly: Boolean): Boolean = raw.setReadable(readable, ownerOnly)
    actual fun setWritable(writable: Boolean, ownerOnly: Boolean): Boolean = raw.setWritable(writable, ownerOnly)
    actual fun setExecutable(executable: Boolean, ownerOnly: Boolean): Boolean = raw.setExecutable(executable, ownerOnly)

    actual fun list(): Array<String>? = raw.list()
    actual fun listFiles(): Array<KxFile>? = raw.listFiles()?.map(::KxFile)?.toTypedArray()

    actual fun listFiles(filter: (KxFile) -> Boolean): Array<KxFile>? = run {
        raw.listFiles { file -> filter(file.toKxFile()) }?.map(::KxFile)?.toTypedArray()
    }

    actual fun readBytes(): ByteArray = raw.readBytes()
    actual fun readText(charset: String): String = raw.readText(Charset.forName(charset))

    actual fun writeBytes(bytes: ByteArray) = raw.writeBytes(bytes)
    actual fun writeText(text: String, charset: String) = raw.writeText(text, Charset.forName(charset))

    actual fun readLines(charset: String): List<String> = raw.readLines(Charset.forName(charset))

    actual override fun toString(): String = absolutePath

    actual fun source(): Source = inputStream().asSource().buffered().peek()
}

fun File.toKxFile(): KxFile = KxFile(this)

fun KxFile.rawFile(): File = File(absolutePath)
fun KxFile.toPath(): Path = rawFile().toPath()
fun Path.toKxFile() = toFile().toKxFile()

fun KxFile.inputStream() = rawFile().inputStream()
fun KxFile.outputStream() = rawFile().outputStream()

actual fun KxFile(path: String): KxFile = KxFile(File(path))
actual fun KxFile(parent: KxFile, child: String): KxFile = KxFile(File(parent.absolutePath, child))
actual fun KxFile(parent: String, child: String): KxFile = KxFile(File(parent, child))
actual fun KxFile(parent: KxFile, child: KxFile): KxFile = KxFile(File(parent.absolutePath, child.name))
