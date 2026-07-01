package com.klyx.api.data.fs

import android.net.Uri
import com.klyx.api.data.file.KxFile
import com.klyx.api.plugin.PluginService
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.InputStream
import java.io.OutputStream

enum class FileCategory {
    TEXT, IMAGE, BINARY_UNSUPPORTED, ERROR
}

interface FileSystem : PluginService {
    suspend fun list(uri: Uri): List<KxFile>
    suspend fun inputStream(uri: Uri): InputStream
    suspend fun outputStream(uri: Uri, mode: String = "w"): OutputStream
    suspend fun delete(uri: Uri): Boolean
    suspend fun rename(uri: Uri, newName: String): Uri?
    suspend fun createFile(parent: Uri, name: String, mimeType: String = "*/*"): Uri?
    suspend fun createDirectory(parent: Uri, name: String): Uri?
    suspend fun exists(uri: Uri): Boolean
    suspend fun copy(source: Uri, targetParent: Uri): Uri?
    suspend fun move(source: Uri, sourceParent: Uri, targetParent: Uri): Uri?
    suspend fun capabilities(uri: Uri): FileCapabilities
    suspend fun fileName(uri: Uri): String?
    suspend fun wrapUri(uri: Uri): KxFile
    suspend fun determineFileCategory(uri: Uri): FileCategory
    suspend fun search(roots: List<Uri>, query: String, maxResults: Int = 500): Flow<KxFile>
}

fun KxFile.createDirIfMissing(): Boolean {
    return if (!exists) mkdirs() else true
}

fun File.createDirIfMissing(): Boolean {
    return if (!exists()) mkdirs() else true
}
