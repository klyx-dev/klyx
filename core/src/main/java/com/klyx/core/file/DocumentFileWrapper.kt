package com.klyx.core.file

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

data class DocumentFileWrapper(
    val raw: DocumentFile,
    private val isDocumentTree: Boolean = false
) : FileWrapper {
    private val _name by lazy { raw.name ?: "(unknown)" }
    private val _isDirectory by lazy { raw.isDirectory }
    private val _isFile by lazy { raw.isFile }

    override val absolutePath get() = raw.uri.toString()
    override val canonicalPath get() = raw.uri.toString()
    override val name get() = _name
    override val path get() = raw.uri.path ?: "UNKNOWN"
    override val mimeType get() = raw.type
    override val parent get() = raw.parentFile?.uri?.toString()
    override val parentFile get() = raw.parentFile?.let(::DocumentFileWrapper)
    override val isFile get() = _isFile
    override val isDirectory get() = _isDirectory
    override val canRestoreFromPath get() = false
    override val id get() = "$name:$length:$lastModified"
    override val length get() = raw.length()
    override val lastModified get() = raw.lastModified()

    override fun asRawFile(): File? = null

    override fun uri(context: Context): Uri = raw.uri
    override fun canRead() = raw.canRead()
    override fun canWrite() = raw.canWrite()
    override fun exists() = raw.exists()
    override fun list(): Array<out String> {
        return raw.listFiles().map { it.name!! }.toTypedArray()
    }

    override fun listFiles(): List<DocumentFileWrapper> {
        return raw.listFiles().map(::DocumentFileWrapper)
    }

    override fun listFiles(filter: (FileWrapper) -> Boolean): List<FileWrapper> {
        return raw.listFiles().map(::DocumentFileWrapper).filter(filter)
    }

    override fun listFiles(filter: (FileWrapper) -> Boolean, recursive: Boolean): List<FileWrapper>? {
        return raw.listFiles().map(::DocumentFileWrapper).filter(filter).let { files ->
            if (recursive) {
                files.flatMap { it.listFiles(filter, recursive) ?: emptyList() }
            } else {
                files
            }
        }
    }

    override fun write(context: Context, content: String): Boolean = runCatching {
        val os = context.contentResolver.openOutputStream(raw.uri) ?: return@runCatching false
        os.bufferedWriter().use { it.write(content) }
        true
    }.getOrElse { false }

    override fun readText(context: Context): String? = context.contentResolver.openInputStream(raw.uri)?.bufferedReader().use {
        it?.readText()
    }

    fun isFromTermux() = raw.uri.host == "com.termux.documents"

    companion object {
        fun shouldWrap(uri: Uri) = ContentResolver.SCHEME_CONTENT == uri.scheme && "com.termux.documents" == uri.host
    }
}
