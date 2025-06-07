package com.klyx.core.file

import android.content.Context
import android.net.Uri
import java.io.File

interface FileWrapper {
    val absolutePath: String
    val canonicalPath: String

    val name: String
    val path: String
    val mimeType: String?
    val parent: String?
    val parentFile: FileWrapper?
    val isFile: Boolean
    val isDirectory: Boolean
    val canRestoreFromPath: Boolean
    val id: FileId
    val length: Long
    val lastModified: Long

    fun asRawFile(): File?
    fun uri(context: Context): Uri

    fun canRead(): Boolean
    fun canWrite(): Boolean
    fun exists(): Boolean

    fun list(): Array<out String>?
    fun listFiles(): List<FileWrapper>?
    fun listFiles(filter: (FileWrapper) -> Boolean): List<FileWrapper>?
    fun listFiles(filter: (FileWrapper) -> Boolean, recursive: Boolean): List<FileWrapper>?

    fun write(context: Context, content: String): Boolean
    fun readText(context: Context): String?
}

val FileWrapper.extension get() = name.substringAfterLast(".", "")
val FileWrapper.nameWithoutExtension get() = name.substringBeforeLast(".")

fun File.wrapFile() = JavaFileWrapper(this)
fun FileWrapper.inputStream(context: Context) = context.contentResolver.openInputStream(uri(context))
