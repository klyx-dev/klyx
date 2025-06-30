package com.klyx.core.file

import android.content.Context
import android.util.Log
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import com.klyx.ifNull
import com.klyx.nothing
import com.klyx.unsupported
import kotlinx.io.Source
import kotlinx.io.asInputStream
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.io.readString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.file.Path

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual open class KxFile(
    private val raw: DocumentFile,
) : KoinComponent {
    private val context: Context by inject()
    private val file = runCatching { raw.uri.toFile() }.getOrNull()

    actual val name: String get() = file?.name ?: raw.name.ifNull { "(unknown)" }
    actual val path: String get() = file?.path ?: raw.uri.path.ifNull { unsupported("KxFile.path is not supported.") }
    actual val absolutePath: String get() = file?.absolutePath ?: path
    actual val parent: String? get() = file?.parent ?: raw.parentFile?.uri?.toString()
    actual val parentFile: KxFile? get() = file?.parentFile?.let(::KxFile) ?: raw.parentFile?.let(::KxFile)
    actual val exists: Boolean get() = file?.exists() ?: raw.exists()
    actual val canRead: Boolean get() = file?.canRead() ?: raw.canRead()
    actual val canWrite: Boolean get() = file?.canWrite() ?: raw.canWrite()
    actual val canExecute: Boolean get() = file?.canExecute() ?: nothing()
    actual val length: Long get() = file?.length() ?: raw.length()
    actual val lastModified: Long get() = file?.lastModified() ?: raw.lastModified()
    actual val extension: String get() = file?.extension ?: name.substringAfterLast(".")
    actual val isHidden: Boolean get() = file?.isHidden ?: nothing()
    actual val isFile: Boolean get() = file?.isFile ?: raw.isFile
    actual val isDirectory: Boolean get() = file?.isDirectory ?: raw.isDirectory

    actual fun mkdirs(): Boolean = file?.mkdirs() ?: unsupported()
    actual fun mkdir(): Boolean = file?.mkdir() ?: unsupported()
    actual fun createNewFile(): Boolean = file?.createNewFile() ?: unsupported()
    actual fun delete(): Boolean = file?.delete() ?: raw.delete()
    actual fun deleteRecursively(): Boolean = file?.deleteRecursively() ?: raw.deleteRecursively()
    actual fun renameTo(dest: KxFile): Boolean = file?.renameTo(File(dest.absolutePath)) ?: raw.renameTo(dest.name)

    actual fun setReadable(readable: Boolean, ownerOnly: Boolean): Boolean = file?.setReadable(readable, ownerOnly) ?: unsupported()
    actual fun setWritable(writable: Boolean, ownerOnly: Boolean): Boolean = file?.setWritable(writable, ownerOnly) ?: unsupported()
    actual fun setExecutable(executable: Boolean, ownerOnly: Boolean): Boolean = file?.setExecutable(executable, ownerOnly) ?: unsupported()

    actual fun list(): Array<String>? {
        return file?.list() ?: raw.listFiles().mapNotNull { it.name }.toTypedArray()
    }

    actual fun listFiles(): Array<KxFile>? {
        return file?.listFiles()?.map { KxFile(it) }?.toTypedArray() ?: raw.listFiles().map(::KxFile).toTypedArray()
    }

    actual fun listFiles(filter: (KxFile) -> Boolean): Array<KxFile>? = run {
        file?.listFiles { it -> filter(KxFile(it)) }?.map { KxFile(it) }?.toTypedArray()
            ?: raw.listFiles().map(::KxFile).filter(filter).toTypedArray()
    }

    actual fun readBytes(): ByteArray = source().readByteArray()

    actual fun readText(charset: String): String = source().readString(Charset.forName(charset))

    actual fun writeBytes(bytes: ByteArray) {
        file?.writeBytes(bytes) ?: outputStream()?.use { it.write(bytes) }
    }

    actual fun writeText(text: String, charset: String) {
        (file?.bufferedWriter(Charset.forName(charset))
            ?: outputStream()?.bufferedWriter(Charset.forName(charset)))?.use { it.write(text) }
    }

    actual fun readLines(charset: String): List<String> {
        return source().asInputStream().bufferedReader(Charset.forName(charset)).use(BufferedReader::readLines)
    }

    actual override fun toString(): String = absolutePath

    fun inputStream(): InputStream? = file?.inputStream() ?: context.contentResolver.openInputStream(raw.uri)
    fun outputStream(): OutputStream? = file?.outputStream() ?: context.contentResolver.openOutputStream(raw.uri)

    fun isFromTermux() = raw.uri.host == "com.termux.documents"
    fun canWatchFileEvents() = file != null

    actual fun source(): Source {
        val input = inputStream() ?: nothing("Failed to open input stream for $absolutePath")
        return input.asSource().buffered().peek()
    }
}

private fun DocumentFile.deleteRecursively(): Boolean {
    if (!exists()) return true // Already deleted or non-existent

    return try {
        if (isDirectory) {
            val children = listFiles()
            var success = true
            for (child in children) {
                success = success && child.deleteRecursively()
            }
            success && delete()
        } else {
            delete()
        }
    } catch (e: Exception) {
        Log.e("KxFile", "Error deleting ${uri}: ${e.message}")
        false
    }
}

fun DocumentFile.toKxFile(): KxFile = KxFile(this)

private fun emptyByteArray() = ByteArray(0)

fun File.toKxFile(): KxFile = KxFile(absolutePath)

fun KxFile.rawFile(): File = File(absolutePath)
fun KxFile.toPath(): Path = rawFile().toPath()
fun Path.toKxFile() = toFile().toKxFile()

fun File.asDocumentFile(): DocumentFile = DocumentFile.fromFile(this)
fun KxFile(file: File): KxFile = KxFile(file.asDocumentFile())

actual fun KxFile(path: String): KxFile = KxFile(File(path))
actual fun KxFile(parent: KxFile, child: String): KxFile = KxFile(File(parent.absolutePath, child))
actual fun KxFile(parent: String, child: String): KxFile = KxFile(File(parent, child))
actual fun KxFile(parent: KxFile, child: KxFile): KxFile = KxFile(File(parent.absolutePath, parent.name))
