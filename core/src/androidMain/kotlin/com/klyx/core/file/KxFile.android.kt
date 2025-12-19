package com.klyx.core.file

import android.content.ContentResolver
import android.content.Context
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.blankj.utilcode.util.UriUtils
import com.klyx.core.ContextHolder
import com.klyx.core.logging.log
import com.klyx.core.terminal.SAFUtils.getDocumentIdForUri
import com.klyx.core.terminal.SAFUtils.getFileForDocumentId
import com.klyx.core.unimplemented
import com.klyx.core.unsupported
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.asInputStream
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable(with = KxFileSerializer::class)
actual open class KxFile(internal val raw: DocumentFile) : KoinComponent {
    private val context: Context by inject()
    private val file = runCatching { raw.uri.toFile() }.getOrNull()

    actual val name: String get() = file?.name ?: raw.name ?: "(unknown)"
    actual val path: String
        get() = file?.path ?: raw.uri.path ?: unsupported("KxFile.path is not supported.")
    actual val absolutePath: String get() = file?.absolutePath ?: path
    actual val parent: String? get() = file?.parent ?: raw.parentFile?.uri?.toString()
    actual val parentFile: KxFile?
        get() = file?.parentFile?.let(::KxFile) ?: raw.parentFile?.let(::KxFile)
    actual val exists: Boolean get() = file?.exists() ?: raw.exists()
    actual val canRead: Boolean get() = file?.canRead() ?: raw.canRead()
    actual val canWrite: Boolean get() = file?.canWrite() ?: raw.canWrite()
    actual val canExecute: Boolean get() = file?.canExecute() ?: unimplemented()
    actual val length: Long get() = file?.length() ?: raw.length()
    actual val lastModified: Long get() = file?.lastModified() ?: raw.lastModified()
    actual val extension: String get() = file?.extension ?: name.substringAfterLast(".", "")
    actual val isHidden: Boolean get() = file?.isHidden ?: unimplemented()
    actual val isFile: Boolean get() = file?.isFile ?: raw.isFile
    actual val isDirectory: Boolean get() = file?.isDirectory ?: raw.isDirectory

    actual fun mkdirs(): Boolean = file?.mkdirs() ?: unsupported()
    actual fun mkdir(): Boolean = file?.mkdir() ?: unsupported()
    actual fun createNewFile(): Boolean = file?.createNewFile() ?: unsupported()
    actual fun delete(): Boolean = file?.delete() ?: raw.delete()
    actual fun deleteRecursively(): Boolean = file?.deleteRecursively() ?: raw.deleteRecursively()
    actual fun renameTo(dest: KxFile): Boolean =
        file?.renameTo(File(dest.absolutePath)) ?: raw.renameTo(dest.name)

    actual fun setReadable(readable: Boolean, ownerOnly: Boolean): Boolean =
        file?.setReadable(readable, ownerOnly) ?: unsupported()

    actual fun setWritable(writable: Boolean, ownerOnly: Boolean): Boolean =
        file?.setWritable(writable, ownerOnly) ?: unsupported()

    actual fun setExecutable(executable: Boolean, ownerOnly: Boolean): Boolean =
        file?.setExecutable(executable, ownerOnly) ?: unsupported()

    actual fun list(): Array<String>? {
        return file?.list() ?: raw.listFiles().mapNotNull { it.name }.toTypedArray()
    }

    actual fun listFiles(): Array<KxFile>? {
        return file?.listFiles()?.map { KxFile(it) }?.toTypedArray() ?: raw.listFiles()
            .map(::KxFile).toTypedArray()
    }

    actual fun listFiles(filter: (KxFile) -> Boolean): Array<KxFile>? = run {
        file?.listFiles { file -> filter(KxFile(file)) }?.map { KxFile(it) }?.toTypedArray()
            ?: raw.listFiles().map(::KxFile).filter(filter).toTypedArray()
    }

    actual fun readBytes(): ByteArray = source().buffered().use(Source::readByteArray)

    actual fun readText(charset: String): String = source()
        .buffered()
        .use { it.readString(Charset.forName(charset)) }

    actual fun writeBytes(bytes: ByteArray) {
        sink().buffered().use { it.write(bytes) }
    }

    actual fun writeText(text: String, charset: String) {
        (file?.bufferedWriter(Charset.forName(charset))
            ?: outputStream()?.bufferedWriter(Charset.forName(charset)))?.use { it.write(text) }
    }

    actual fun readLines(charset: String): List<String> {
        return source().buffered().asInputStream().bufferedReader(Charset.forName(charset))
            .use(BufferedReader::readLines)
    }

    actual override fun toString(): String = absolutePath

    fun inputStream(): InputStream? =
        file?.inputStream() ?: context.contentResolver.openInputStream(raw.uri)

    fun outputStream(): OutputStream? =
        file?.outputStream() ?: context.contentResolver.openOutputStream(raw.uri)
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

val KxFile.uri: Uri get() = raw.uri
fun DocumentFile.toKxFile(): KxFile = KxFile(this)

fun File.asDocumentFile(): DocumentFile = DocumentFile.fromFile(this)
fun KxFile(file: File): KxFile = KxFile(file.asDocumentFile())

fun KxFile.extractZip(outputDir: File) {
    inputStream()?.use { inputStream ->
        ZipInputStream(BufferedInputStream(inputStream)).use { zipStream ->
            var entry: ZipEntry? = zipStream.nextEntry
            while (entry != null) {
                val file = File(outputDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    file.outputStream().use(zipStream::copyTo)
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }
    }
}

fun Uri.toKxFile() = KxFile(this.toString())

actual fun KxFile(path: String): KxFile {
    val context = ContextHolder.context
    val uri = path.toUri()

    val fallback = {
        if (DocumentsContract.isTreeUri(uri)) {
            DocumentFile.fromTreeUri(context, uri)!!
        } else {
            DocumentFile.fromSingleUri(context, uri)!!
        }
    }

    return when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }.onFailure {
                log.debug { "Failed to take persistable permission for $path: $it" }
            }.onSuccess {
                log.debug { "Successfully took persistable permission for $path" }
            }

            val file = try {
                UriUtils.uri2FileNoCacheCopy(uri).asDocumentFile()
            } catch (_: Throwable) {
                if (uri.host == "com.klyx.documents") {
                    try {
                        getFileForDocumentId(getDocumentIdForUri(uri)).asDocumentFile()
                    } catch (_: Throwable) {
                        fallback()
                    }
                } else {
                    fallback()
                }
            }

            KxFile(file)
        }

        ContentResolver.SCHEME_FILE -> uri.toFile().toKxFile()
        else -> KxFile(File(path))
    }
}

actual fun KxFile.isPermissionRequired(permissionFlags: Int): Boolean {
    return requiresPermission(ContextHolder.context, permissionFlags)
}

actual fun KxFile.source(): RawSource {
    val input = inputStream() ?: throw IOException("Failed to open input stream for $absolutePath")
    return input.asSource()
}

actual fun KxFile.sink(): RawSink {
    val output = outputStream() ?: throw IOException("Failed to open output stream for $absolutePath")
    return output.asSink()
}

actual fun KxFile.mimeType(): String? {
    val context = ContextHolder.context
    val mimeType = context.contentResolver.getType(uri)
        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    return mimeType
}
