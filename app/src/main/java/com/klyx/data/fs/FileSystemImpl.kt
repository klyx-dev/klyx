package com.klyx.data.fs

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document.MIME_TYPE_DIR
import android.provider.OpenableColumns
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.klyx.data.file.KxFile
import com.klyx.data.file.wrap
import org.koin.core.annotation.Single
import java.io.InputStream
import java.io.OutputStream

@Single
class FileSystemImpl(
    private val context: Context
) : FileSystem {

    private val content = context.contentResolver
    private val Uri.file get() = if (scheme == SCHEME_FILE) this.toFile() else null

    override fun list(uri: Uri): List<KxFile> {
        val file = uri.wrap()
        return file.listFiles()
    }

    override fun inputStream(uri: Uri): InputStream {
        return checkNotNull(context.contentResolver.openInputStream(uri)) {
            "the provider recently crashed."
        }
    }

    override fun outputStream(uri: Uri, mode: String): OutputStream {
        return checkNotNull(context.contentResolver.openOutputStream(uri, mode)) {
            "the provider recently crashed."
        }
    }

    override fun delete(uri: Uri) =
        uri.file?.delete() ?: DocumentsContract.deleteDocument(content, uri)

    override fun rename(uri: Uri, newName: String): Uri? {
        val file = uri.file
        if (file != null) {
            val newFile = file.resolveSibling(newName)
            val success = file.renameTo(newFile)
            return if (success) newFile.toUri() else null
        }

        return DocumentsContract.renameDocument(content, uri, newName)
    }

    override fun createFile(parent: Uri, name: String, mimeType: String): Uri? {
        val parentFile = parent.file
        if (parentFile != null) {
            val file = parentFile.resolve(name)
            val success = if (mimeType == MIME_TYPE_DIR) file.mkdirs() else file.createNewFile()
            return if (success) file.toUri() else null
        }
        return DocumentsContract.createDocument(content, parent, mimeType, name)
    }

    override fun createDirectory(parent: Uri, name: String): Uri? {
        return createFile(parent, name, MIME_TYPE_DIR)
    }

    override fun capabilities(uri: Uri): FileCapabilities {
        if (uri.scheme == SCHEME_FILE) {
            val file = uri.toFile()
            return FileCapabilities(
                canWrite = file.canWrite(),
                canCreate = file.canWrite(),
                canDelete = file.canWrite(),
                canRename = file.canWrite()
            )
        }

        val flags = context.getDocumentFlags(uri)

        return FileCapabilities(
            canWrite = flags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE != 0,
            canDelete = flags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE != 0,
            canRename = flags and DocumentsContract.Document.FLAG_SUPPORTS_RENAME != 0,
            canCreate = flags and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE != 0
        )
    }

    override fun fileName(uri: Uri): String? {
        return uri.file?.name ?: uri.query(OpenableColumns.DISPLAY_NAME) { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1 && cursor.moveToFirst()) {
                cursor.getString(index)
            } else uri.lastPathSegment
        }
    }

    override fun exists(uri: Uri): Boolean {
        return uri.file?.exists() ?: content.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            null,
            null,
            null
        )?.use {
            it.count > 0
        } ?: false
    }

    override fun copy(source: Uri, targetParent: Uri): Uri? {
        val sourceFile = source.file
        val targetParentFile = targetParent.file

        if (sourceFile != null && targetParentFile != null) {
            return try {
                val target = targetParentFile.resolve(sourceFile.name)

                sourceFile.inputStream().buffered().use { input ->
                    target.outputStream().buffered().use { output ->
                        input.copyTo(output)
                    }
                }

                target.toUri()
            } catch (_: Exception) {
                null
            }
        }

        return try {
            DocumentsContract.copyDocument(
                content,
                source,
                targetParent
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun move(
        source: Uri,
        sourceParent: Uri,
        targetParent: Uri
    ): Uri? {
        val sourceFile = source.file
        val targetParentFile = targetParent.file

        if (sourceFile != null && targetParentFile != null) {
            return try {
                val target = targetParentFile.resolve(sourceFile.name)

                if (target.exists()) {
                    return null
                }

                if (sourceFile.renameTo(target)) {
                    return target.toUri()
                }

                sourceFile.inputStream().buffered().use { input ->
                    target.outputStream().buffered().use { output ->
                        input.copyTo(output)
                    }
                }

                if (sourceFile.delete()) {
                    target.toUri()
                } else {
                    target.delete()
                    null
                }
            } catch (_: Exception) {
                null
            }
        }

        return try {
            DocumentsContract.moveDocument(
                content,
                source,
                sourceParent,
                targetParent
            )
        } catch (_: Exception) {
            null
        }
    }

    private inline fun <R> Uri.query(projection: String, block: (Cursor) -> R): R {
        val cursor = checkNotNull(content.query(this, arrayOf(projection), null, null, null)) {
            "the underlying content provider returned null, or it crashed."
        }
        return cursor.use(block)
    }
}

private fun Context.getDocumentFlags(uri: Uri): Int {
    val projection = arrayOf(DocumentsContract.Document.COLUMN_FLAGS)

    contentResolver.query(uri, projection, null, null, null)?.use {
        if (it.moveToFirst()) return it.getInt(0)
    }
    return 0
}
