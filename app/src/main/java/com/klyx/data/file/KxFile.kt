package com.klyx.data.file

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.blankj.utilcode.util.UriUtils
import com.klyx.data.fs.Paths
import com.klyx.terminal.home
import com.klyx.util.applicationContext
import com.klyx.util.isFileUri
import com.klyx.util.isTextFile
import com.klyx.util.withApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

@Serializable
class KxFile : java.io.Serializable {

    private val uriString: String

    @Transient
    @kotlin.jvm.Transient
    private var _raw: DocumentFile? = null

    @Transient
    @kotlin.jvm.Transient
    private var originalFile: File? = null

    @Transient
    @kotlin.jvm.Transient
    private var prefetched: PrefetchedFileMetadata? = null

    val raw: DocumentFile
        get() {
            if (_raw == null) {
                _raw = originalFile?.let { DocumentFile.fromFile(it) }
                    ?: uriString.toUri().let { uri ->
                        val context = applicationContext()
                        if (DocumentsContract.isTreeUri(uri)) DocumentFile.fromTreeUri(context, uri)
                        else DocumentFile.fromSingleUri(context, uri)
                    }
            }
            return _raw!!
        }

    val file: File? get() = originalFile ?: if (uri.isFileUri) uri.toFile() else null

    val uri get() = raw.uri

    constructor(raw: DocumentFile) {
        this._raw = raw
        this.uriString = raw.uri.toString()
        this.originalFile = if (raw.uri.isFileUri) raw.uri.toFile() else null
    }

    constructor(file: File) {
        this.originalFile = file
        this.uriString = file.toUri().toString()
    }

    internal constructor(raw: DocumentFile, prefetched: PrefetchedFileMetadata) : this(raw) {
        this.prefetched = prefetched
    }

    val mimeType get() = applicationContext().contentResolver.getType(uri)

    val isSafDocument by lazy { file == null }

    val name get() = prefetched?.name ?: file?.name ?: raw.name ?: error("Invalid file: $uri")
    val path get() = file?.path ?: raw.uri.path ?: error("Invalid uri: $uri")
    val absolutePath get() = file?.absolutePath ?: path

    val parent get() = file?.parent ?: raw.parentFile?.uri?.toString()
    val parentFile get() = file?.parentFile?.let(::KxFile) ?: raw.parentFile?.let(::KxFile)

    val exists get() = file?.exists() ?: raw.exists()
    val canRead get() = file?.canRead() ?: raw.canRead()
    val canWrite get() = file?.canWrite() ?: raw.canWrite()
    val canExecute get() = file?.canExecute() ?: false

    val length get() = prefetched?.size ?: file?.length() ?: raw.length()
    val lastModified get() = prefetched?.lastModified ?: file?.lastModified() ?: raw.lastModified()

    val extension
        get() = prefetched?.extension ?: file?.extension ?: run {
            if (name.startsWith(".") && name.count { it == '.' } == 1) {
                ""
            } else if (name.contains(".")) {
                name.substringAfterLast(".")
            } else {
                ""
            }
        }

    val isHidden get() = prefetched?.isHidden ?: file?.isHidden ?: name.startsWith(".")
    val isFile get() = prefetched?.isFile ?: file?.isFile ?: raw.isFile
    val isDirectory get() = prefetched?.isDirectory ?: file?.isDirectory ?: raw.isDirectory

    fun mkdirs() =
        file?.mkdirs() ?: unsupported("use filesystem for creating directories with SAF uri")

    fun mkdir() = file?.mkdir() ?: unsupported("use filesystem for creating directory with SAF uri")
    fun createNewFile() =
        file?.createNewFile() ?: unsupported("use filesystem for creating a file with SAF uri")

    fun delete() = file?.delete() ?: raw.delete()
    fun deleteRecursively() = file?.deleteRecursively() ?: raw.deleteRecursively()

    fun renameTo(dest: KxFile) = file?.renameTo(File(dest.absolutePath)) ?: raw.renameTo(dest.name)

    fun setReadable(readable: Boolean, ownerOnly: Boolean = true) =
        file?.setReadable(readable, ownerOnly) ?: unsupported()

    fun setWritable(writable: Boolean, ownerOnly: Boolean = true) =
        file?.setWritable(writable, ownerOnly) ?: unsupported()

    fun setExecutable(executable: Boolean, ownerOnly: Boolean = true) =
        file?.setExecutable(executable, ownerOnly) ?: unsupported()

    fun list() = file?.list()?.asList() ?: raw.listFiles().mapNotNull { it.name }
    fun listFiles(filter: (KxFile) -> Boolean = { true }): List<KxFile> {
        return (file?.listFiles()?.map(::KxFile) ?: raw.listFiles().map(::KxFile)).filter(filter)
    }

    fun readBytes() = inputStream().use { it.readBytes() }
    fun readText(charset: Charset = StandardCharsets.UTF_8) =
        inputStream().bufferedReader(charset).use { it.readText() }

    fun writeBytes(bytes: ByteArray) = outputStream().use { it.write(bytes) }
    fun writeText(text: String, charset: Charset = StandardCharsets.UTF_8) =
        outputStream().bufferedWriter(charset).use { it.write(text) }

    fun readLines(charset: Charset = StandardCharsets.UTF_8) =
        inputStream().bufferedReader(charset).use { it.readLines() }

    override fun toString() = absolutePath

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KxFile) return false
        return uri == other.uri
    }

    override fun hashCode() = uri.hashCode()

    fun inputStream() = withApplicationContext {
        file?.inputStream()
            ?: checkNotNull(contentResolver.openInputStream(raw.uri)) {
                "InputStream is null. Probably the provider recently crashed."
            }
    }

    fun outputStream() = withApplicationContext {
        file?.outputStream()
            ?: checkNotNull(contentResolver.openOutputStream(raw.uri)) {
                "OutputStream is null. Probably the provider recently crashed."
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
}

internal data class PrefetchedFileMetadata(
    val name: String,
    val mimeType: String?,
    val size: Long,
    val lastModified: Long,
    val flags: Int
) {
    val isDirectory: Boolean get() = DocumentsContract.Document.MIME_TYPE_DIR == mimeType
    val isFile: Boolean get() = !isDirectory

    val extension: String get() = when {
        name.startsWith(".") && name.count { it == '.' } == 1 -> ""
        name.contains(".") -> name.substringAfterLast(".")
        else -> ""
    }

    val isHidden: Boolean get() = name.startsWith(".")
}

@Suppress("NOTHING_TO_INLINE")
inline fun File.wrap() = KxFile(this)

@Suppress("NOTHING_TO_INLINE")
inline fun Uri.wrap() = KxFile(this)

@Suppress("NOTHING_TO_INLINE")
inline fun DocumentFile.wrap() = KxFile(this)

fun KxFile(path: String) = KxFile(File(path))

fun KxFile(uri: Uri): KxFile {
    val context = applicationContext()

    val localFile = resolveFromOwnProvider(uri)
    if (localFile != null) return KxFile(localFile)

    val fallback = {
        if (DocumentsContract.isTreeUri(uri)) {
            DocumentFile.fromTreeUri(context, uri)!!
        } else {
            DocumentFile.fromSingleUri(context, uri)!!
        }
    }

    return when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> {
            val resolvedFile = try {
                UriUtils.uri2FileNoCacheCopy(uri)
            } catch (_: SecurityException) {
                null
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }

            if (DocumentsContract.isTreeUri(uri)) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: Throwable) {
                }
            }

            if (resolvedFile != null) {
                KxFile(resolvedFile)
            } else {
                KxFile(fallback())
            }
        }

        ContentResolver.SCHEME_FILE -> KxFile(uri.toFile())
        else -> KxFile(fallback())
    }
}

private fun unsupported(message: String? = null): Nothing =
    throw UnsupportedOperationException(message)

private fun resolveFromOwnProvider(uri: Uri): File? {
    val context = applicationContext()
    val providerAuthority = "${context.packageName}.terminal.documents"
    if (uri.authority != providerAuthority) return null
    return when {
        DocumentsContract.isTreeUri(uri) -> File(DocumentsContract.getTreeDocumentId(uri))
        DocumentsContract.isDocumentUri(context, uri) -> File(DocumentsContract.getDocumentId(uri))
        else -> null
    }
}

fun KxFile.resolveName(): String {
    return when (absolutePath) {
        Environment.getExternalStorageDirectory().absolutePath -> "Internal Storage"
        Paths.dataDir.absolutePath -> "App Data"
        Paths.home.absolutePath -> "Terminal Home"
        else -> name
    }
}

suspend fun KxFile.isTextFile() = isTextFile(uri, applicationContext().contentResolver)
