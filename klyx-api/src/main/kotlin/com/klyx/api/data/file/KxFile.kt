package com.klyx.api.data.file

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.blankj.utilcode.util.UriUtils
import com.klyx.api.util.isFileUri
import com.klyx.api.util.toFile
import com.klyx.api.util.toUri
import com.klyx.core.koin
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * A unified file representation that wraps both standard [File] and Android's [DocumentFile].
 *
 * `KxFile` provides a consistent API for file operations, regardless of whether the underlying
 * storage is a local file system or a provider accessible via the Storage Access Framework (SAF).
 *
 * It is [Serializable], allowing it to be passed between components or saved in state,
 * although the internal [DocumentFile] reference is transient and reconstructed from the URI.
 */
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

    /**
     * The underlying [DocumentFile] for SAF operations.
     */
    val raw: DocumentFile
        get() {
            if (_raw == null) {
                _raw = originalFile?.let { DocumentFile.fromFile(it) }
                    ?: uriString.toUri().let { uri ->
                        val context: Context by koin()
                        if (DocumentsContract.isTreeUri(uri)) DocumentFile.fromTreeUri(context, uri)
                        else DocumentFile.fromSingleUri(context, uri)
                    }
            }
            return _raw!!
        }

    /**
     * The underlying [File], or null if this is a pure SAF/content URI.
     */
    val file: File? get() = originalFile ?: if (uri.isFileUri) uri.toFile() else null

    /**
     * The [Uri] representing this file.
     */
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

    constructor(raw: DocumentFile, prefetched: PrefetchedFileMetadata) : this(raw) {
        this.prefetched = prefetched
    }

    /** The MIME type of the file. */
    val mimeType get() = context().contentResolver.getType(uri)

    /** Whether this file is managed via Storage Access Framework. */
    val isSafDocument by lazy { file == null }

    /** The display name of the file. */
    val name get() = prefetched?.name ?: file?.name ?: raw.name ?: error("Invalid file: $uri")

    /** The path string of the file. */
    val path get() = file?.path ?: raw.uri.path ?: error("Invalid uri: $uri")

    /** The absolute path string of the file. */
    val absolutePath get() = file?.absolutePath ?: path

    /** The path string of the parent directory. */
    val parent get() = file?.parent ?: raw.parentFile?.uri?.toString()

    /** The parent directory as a [KxFile]. */
    val parentFile get() = file?.parentFile?.let(::KxFile) ?: raw.parentFile?.let(::KxFile)

    /** Whether the file or directory exists. */
    val exists get() = file?.exists() ?: raw.exists()

    /** Whether the file is readable. */
    val canRead get() = file?.canRead() ?: raw.canRead()

    /** Whether the file is writable. */
    val canWrite get() = file?.canWrite() ?: raw.canWrite()

    /** Whether the file is executable (only applicable for local files). */
    val canExecute get() = file?.canExecute() ?: false

    /** The size of the file in bytes. */
    val length get() = prefetched?.size ?: file?.length() ?: raw.length()

    /** The time that the file was last modified, in milliseconds since the epoch. */
    val lastModified get() = prefetched?.lastModified ?: file?.lastModified() ?: raw.lastModified()

    /** The file extension (e.g., "kt" or "txt"). */
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

    /** Whether the file is considered hidden. */
    val isHidden get() = prefetched?.isHidden ?: file?.isHidden ?: name.startsWith(".")

    /** Whether this is a regular file. */
    val isFile get() = prefetched?.isFile ?: file?.isFile ?: raw.isFile

    /** Whether this is a directory. */
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

    /** Reads the entire content of the file as a [ByteArray]. */
    fun readBytes() = inputStream().use { it.readBytes() }

    /** Reads the entire content of the file as a [String] using the specified [charset]. */
    fun readText(charset: Charset = StandardCharsets.UTF_8) =
        inputStream().bufferedReader(charset).use { it.readText() }

    /** Writes [bytes] to the file. */
    fun writeBytes(bytes: ByteArray) = outputStream().use { it.write(bytes) }

    /** Writes [text] to the file using the specified [charset]. */
    fun writeText(text: String, charset: Charset = StandardCharsets.UTF_8) =
        outputStream().bufferedWriter(charset).use { it.write(text) }

    /** Reads the entire content of the file as a list of lines. */
    fun readLines(charset: Charset = StandardCharsets.UTF_8) =
        inputStream().bufferedReader(charset).use { it.readLines() }

    override fun toString() = absolutePath

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KxFile) return false
        return uri == other.uri
    }

    override fun hashCode() = uri.hashCode()

    /** Opens an [InputStream] for this file. */
    fun inputStream() = context().contentResolver.run {
        file?.inputStream()
            ?: checkNotNull(openInputStream(this@KxFile.uri)) {
                "InputStream is null. Probably the provider recently crashed."
            }
    }

    /** Opens an [OutputStream] for this file. */
    fun outputStream() = context().contentResolver.run {
        file?.outputStream()
            ?: checkNotNull(openOutputStream(this@KxFile.uri)) {
                "OutputStream is null. Probably the provider recently crashed."
            }
    }

    private fun DocumentFile.deleteRecursively(): Boolean {
        if (!exists()) return true

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

private fun context(): Context {
    val context: Context by koin()
    return context
}

/**
 * Immutable metadata for a file, typically prefetched during directory listing
 * to avoid expensive individual queries to content providers.
 */
data class PrefetchedFileMetadata(
    val name: String,
    val mimeType: String?,
    val size: Long,
    val lastModified: Long,
    val flags: Int
) {

    /** Whether this metadata describes a directory. */
    val isDirectory: Boolean get() = DocumentsContract.Document.MIME_TYPE_DIR == mimeType

    /** Whether this metadata describes a regular file. */
    val isFile: Boolean get() = !isDirectory

    /** The file extension derived from the name. */
    val extension: String
        get() = when {
            name.startsWith(".") && name.count { it == '.' } == 1 -> ""
            name.contains(".") -> name.substringAfterLast(".")
            else -> ""
        }

    /** Whether the file name starts with a dot. */
    val isHidden: Boolean get() = name.startsWith(".")
}

/** Wraps this [File] into a [KxFile]. */
@Suppress("NOTHING_TO_INLINE")
inline fun File.wrap() = KxFile(this)

/** Wraps this [Uri] into a [KxFile]. */
@Suppress("NOTHING_TO_INLINE")
inline fun Uri.wrap() = KxFile(this)

/** Wraps this [DocumentFile] into a [KxFile]. */
@Suppress("NOTHING_TO_INLINE")
inline fun DocumentFile.wrap() = KxFile(this)

/** Creates a [KxFile] from a local path string. */
fun KxFile(path: String) = KxFile(File(path))

/**
 * Resolves a [Uri] into a [KxFile], attempting to determine if it is a local file
 * or a content provider URI.
 */
fun KxFile(uri: Uri): KxFile {
    val context by koin<Context>()

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
    val context by koin<Context>()
    val providerAuthority = "${context.packageName}.terminal.documents"
    if (uri.authority != providerAuthority) return null
    return when {
        DocumentsContract.isTreeUri(uri) -> File(DocumentsContract.getTreeDocumentId(uri))
        DocumentsContract.isDocumentUri(context, uri) -> File(DocumentsContract.getDocumentId(uri))
        else -> null
    }
}

/**
 * Resolves a human-readable name for common directories (e.g., "Terminal Home").
 */
fun KxFile.resolveName(): String {
    val context by koin<Context>()
    return when (absolutePath) {
        Environment.getExternalStorageDirectory().absolutePath -> "Internal Storage"
        context.dataDir.absolutePath -> "App Data"
        context.filesDir.resolve("home").absolutePath,
        context.filesDir.resolve("home").canonicalPath,
            -> "Terminal Home"

        else -> name
    }
}

/**
 * The key used to look up a [LanguageServerProvider][com.klyx.api.lsp.LanguageServerProvider] in the registry.
 * Falls back to the lowercased file name for extensionless files
 * (e.g. "dockerfile", "makefile").
 */
val KxFile.providerKey: String get() = extension.ifBlank { name.lowercase() }
