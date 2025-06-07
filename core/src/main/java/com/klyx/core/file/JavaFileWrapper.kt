package com.klyx.core.file

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

data class JavaFileWrapper(
    private val raw: File
) : FileWrapper {
    override val absolutePath: String get() = raw.absolutePath
    override val canonicalPath: String get() = raw.canonicalPath
    override val name: String get() = raw.name
    override val path: String get() = raw.path
    override val mimeType get() = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    override val parent: String? get() = raw.parent
    override val parentFile get() = raw.parentFile?.wrapFile()
    override val isFile get() = raw.isFile
    override val isDirectory get() = raw.isDirectory
    override val canRestoreFromPath get() = true
    override val id get() = raw.id
    override val length get() = raw.length()
    override val lastModified get() = raw.lastModified()

    override fun asRawFile(): File = raw

    override fun uri(context: Context): Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", raw)
    override fun canRead() = raw.canRead()
    override fun canWrite() = raw.canWrite()
    override fun exists() = raw.exists()
    override fun list(): Array<out String>? = raw.list()

    override fun listFiles(): List<FileWrapper>? {
        return raw.listFiles()?.map { it.wrapFile() }
    }

    override fun listFiles(filter: (FileWrapper) -> Boolean): List<FileWrapper>? {
        return raw.listFiles()?.map { it.wrapFile() }?.filter(filter)
    }

    override fun listFiles(filter: (FileWrapper) -> Boolean, recursive: Boolean): List<FileWrapper>? {
        return raw.listFiles()?.map { it.wrapFile() }?.filter(filter)?.let { files ->
            if (recursive) {
                files.flatMap { it.listFiles(filter, recursive) ?: emptyList() }
            } else {
                files
            }
        }
    }

    override fun write(context: Context, content: String): Boolean = runCatching {
        raw.writeText(content)
        true
    }.getOrElse {
        false
    }

    override fun readText(context: Context): String? = runCatching { raw.readText() }.getOrNull()
}

