package com.klyx.data.file

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.klyx.data.fs.FileSystem
import com.klyx.util.isTextFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

enum class FileCategory {
    TEXT, IMAGE, BINARY_UNSUPPORTED, ERROR
}

@Single
class FileRepository(
    private val context: Context,
    private val fs: FileSystem
) {

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    suspend fun rename(uri: Uri, newName: String) = withContext(Dispatchers.IO) {
        fs.rename(uri, newName)
    }

    suspend fun delete(uri: Uri) = withContext(Dispatchers.IO) {
        fs.delete(uri)
    }

    suspend fun createFile(parent: Uri, fileName: String) = withContext(Dispatchers.IO) {
        fs.createFile(parent, fileName, "*/*")
    }

    suspend fun createDirectory(parent: Uri, dirName: String) = withContext(Dispatchers.IO) {
        fs.createDirectory(parent, dirName)
    }

    suspend fun copy(source: Uri, targetParent: Uri) = withContext(Dispatchers.IO) {
        fs.copy(source, targetParent)
    }

    suspend fun move(source: Uri, sourceParent: Uri, targetParent: Uri) = withContext(Dispatchers.IO) {
        fs.move(source, sourceParent, targetParent)
    }

    /**
     * Wraps the URI into [KxFile].
     */
    suspend fun wrapUri(uri: Uri) = withContext(Dispatchers.IO) { uri.wrap() }

    suspend fun determineFileCategory(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val mimeType = contentResolver.getType(uri)
                ?: MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(
                        MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                            ?.lowercase()
                    )

            if (mimeType?.startsWith("image/") == true) {
                return@withContext FileCategory.IMAGE
            }

            if (isTextFile(uri, contentResolver)) FileCategory.TEXT else FileCategory.BINARY_UNSUPPORTED
        } catch (_: Exception) {
            FileCategory.ERROR
        }
    }
}
