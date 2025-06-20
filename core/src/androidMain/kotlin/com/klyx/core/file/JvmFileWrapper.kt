package com.klyx.core.file

import android.webkit.MimeTypeMap
import java.io.File

class JvmFileWrapper(
    private val raw: File
) : FileWrapper {
    override val absolutePath: String = raw.absolutePath
    override val canonicalPath: String = raw.canonicalPath
    override val name: String = raw.name
    override val path: String = raw.path
    override val mimeType: String = MimeTypeMap.getSingleton().getMimeTypeFromExtension(raw.extension) ?: ""
    override val parent: String? = raw.parent
    override val parentFile: JvmFileWrapper? = raw.parentFile?.let(::JvmFileWrapper)
    override val isFile: Boolean = raw.isFile
    override val isDirectory: Boolean = raw.isDirectory
    override val canRestoreFromPath: Boolean = true
    override val id: FileId = raw.id
    override val length: Long = raw.length()
    override val lastModified: Long = raw.lastModified()

    override fun canRead() = raw.canRead()
    override fun canWrite() = raw.canWrite()
    override fun exists() = raw.exists()

    override fun list(): Array<String>? = raw.list()

    override fun listFiles(): List<JvmFileWrapper>? {
        return raw.listFiles()?.map(::JvmFileWrapper)
    }

    override fun listFiles(filter: (FileWrapper) -> Boolean): List<FileWrapper>? {
        return raw.listFiles()?.map(::JvmFileWrapper)?.filter(filter)
    }

    override fun listFiles(filter: (FileWrapper) -> Boolean, recursive: Boolean): List<FileWrapper>? {
        return raw.listFiles()?.map(::JvmFileWrapper)?.filter(filter)?.let { files ->
            if (recursive) {
                files.flatMap {
                    @Suppress("KotlinConstantConditions")
                    it.listFiles(filter, recursive) ?: emptyList()
                }
            } else {
                files
            }
        }
    }

    override fun readText() = raw.readText()

    override fun writeText(text: String) = try {
        raw.writeText(text)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun JvmFileWrapper.asJvmFile() = File(absolutePath)
fun File.wrapFile() = JvmFileWrapper(this)
