package com.klyx.data.fs

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Document.MIME_TYPE_DIR
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.klyx.data.file.KxFile
import com.klyx.data.file.PrefetchedFileMetadata
import com.klyx.data.file.wrap
import com.klyx.util.isTextFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File
import java.io.InputStream
import java.io.OutputStream

@Single
class FileSystemImpl(
    private val context: Context
) : FileSystem {

    private val content = context.contentResolver

    override suspend fun list(uri: Uri): List<KxFile> = withContext(Dispatchers.IO) {
        val localFile = uri.resolveToLocalFile()
        if (localFile != null) {
            localFile.listFiles()?.map(::KxFile).orEmpty()
        } else if (DocumentsContract.isTreeUri(uri)) {
            listSafBatched(uri)
        } else {
            uri.wrap().listFiles()
        }
    }

    private fun Uri.resolveToLocalFile(): File? {
        if (scheme == "file") return toFile()
        val providerAuthority = "${context.packageName}.terminal.documents"
        if (authority != providerAuthority) return null
        return when {
            DocumentsContract.isTreeUri(this) -> File(DocumentsContract.getTreeDocumentId(this))
            DocumentsContract.isDocumentUri(context, this) -> File(DocumentsContract.getDocumentId(this))
            else -> null
        }
    }

    private fun listSafBatched(treeUri: Uri): List<KxFile> {
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
        val columns = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS
        )

        val cursor = content.query(childrenUri, columns, null, null, null)
        cursor?.use { c ->
            val idIdx = c.getColumnIndex(Document.COLUMN_DOCUMENT_ID)
            val nameIdx = c.getColumnIndex(Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = c.getColumnIndex(Document.COLUMN_MIME_TYPE)
            val sizeIdx = c.getColumnIndex(Document.COLUMN_SIZE)
            val dateIdx = c.getColumnIndex(Document.COLUMN_LAST_MODIFIED)
            val flagsIdx = c.getColumnIndex(Document.COLUMN_FLAGS)

            val results = mutableListOf<KxFile>()
            while (c.moveToNext()) {
                val docId = c.getString(idIdx)
                val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                val raw = DocumentFile.fromSingleUri(context, childUri)!!

                val name = if (nameIdx >= 0) c.getString(nameIdx) ?: docId else docId
                val metadata = PrefetchedFileMetadata(
                    name = name,
                    mimeType = if (mimeIdx >= 0) c.getString(mimeIdx) else null,
                    size = if (sizeIdx >= 0) c.getLong(sizeIdx) else 0L,
                    lastModified = if (dateIdx >= 0) c.getLong(dateIdx) else 0L,
                    flags = if (flagsIdx >= 0) c.getInt(flagsIdx) else 0
                )
                results.add(KxFile(raw, metadata))
            }
            return results
        }

        return treeUri.wrap().listFiles()
    }

    override suspend fun inputStream(uri: Uri): InputStream = withContext(Dispatchers.IO) {
        checkNotNull(context.contentResolver.openInputStream(uri)) {
            "the provider recently crashed."
        }
    }

    override suspend fun outputStream(uri: Uri, mode: String): OutputStream = withContext(Dispatchers.IO) {
        checkNotNull(context.contentResolver.openOutputStream(uri, mode)) {
            "the provider recently crashed."
        }
    }

    override suspend fun delete(uri: Uri) = withContext(Dispatchers.IO) {
        uri.file?.delete() ?: DocumentsContract.deleteDocument(content, uri)
    }

    override suspend fun rename(uri: Uri, newName: String): Uri? = withContext(Dispatchers.IO) {
        val file = uri.file
        if (file != null) {
            val newFile = file.resolveSibling(newName)
            val success = file.renameTo(newFile)
            return@withContext if (success) newFile.toUri() else null
        }

        DocumentsContract.renameDocument(content, uri, newName)
    }

    override suspend fun createFile(parent: Uri, name: String, mimeType: String): Uri? = withContext(Dispatchers.IO) {
        val parentFile = parent.file
        if (parentFile != null) {
            val file = parentFile.resolve(name)
            val success = if (mimeType == MIME_TYPE_DIR) file.mkdirs() else file.createNewFile()
            return@withContext if (success) file.toUri() else null
        }
        DocumentsContract.createDocument(content, parent, mimeType, name)
    }

    override suspend fun createDirectory(parent: Uri, name: String): Uri? = withContext(Dispatchers.IO) {
        createFile(parent, name, MIME_TYPE_DIR)
    }

    override suspend fun capabilities(uri: Uri): FileCapabilities = withContext(Dispatchers.IO) {
        if (uri.scheme == "file") {
            val file = uri.toFile()
            return@withContext FileCapabilities(
                canWrite = file.canWrite(),
                canCreate = file.canWrite(),
                canDelete = file.canWrite(),
                canRename = file.canWrite()
            )
        }

        val flags = getDocumentFlags(uri)

        FileCapabilities(
            canWrite = flags and Document.FLAG_SUPPORTS_WRITE != 0,
            canDelete = flags and Document.FLAG_SUPPORTS_DELETE != 0,
            canRename = flags and Document.FLAG_SUPPORTS_RENAME != 0,
            canCreate = flags and Document.FLAG_DIR_SUPPORTS_CREATE != 0
        )
    }

    override suspend fun fileName(uri: Uri): String? = withContext(Dispatchers.IO) {
        uri.file?.name ?: uri.query(OpenableColumns.DISPLAY_NAME) { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1 && cursor.moveToFirst()) {
                cursor.getString(index)
            } else uri.lastPathSegment
        }
    }

    override suspend fun exists(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        uri.file?.exists() ?: content.query(
            uri,
            arrayOf(Document.COLUMN_DOCUMENT_ID),
            null,
            null,
            null
        )?.use {
            it.count > 0
        } ?: false
    }

    override suspend fun copy(source: Uri, targetParent: Uri): Uri? = withContext(Dispatchers.IO) {
        val sourceFile = source.file
        val targetParentFile = targetParent.file

        if (sourceFile != null && targetParentFile != null) {
            return@withContext try {
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

        try {
            DocumentsContract.copyDocument(
                content,
                source,
                targetParent
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun move(
        source: Uri,
        sourceParent: Uri,
        targetParent: Uri
    ): Uri? = withContext(Dispatchers.IO) {
        val sourceFile = source.file
        val targetParentFile = targetParent.file

        if (sourceFile != null && targetParentFile != null) {
            return@withContext try {
                val target = targetParentFile.resolve(sourceFile.name)

                if (target.exists()) {
                    return@withContext null
                }

                if (sourceFile.renameTo(target)) {
                    return@withContext target.toUri()
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

        try {
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

    override suspend fun wrapUri(uri: Uri): KxFile = withContext(Dispatchers.IO) {
        uri.wrap()
    }

    override suspend fun determineFileCategory(uri: Uri): FileCategory {
        val cr = context.contentResolver
        return try {
            val mimeType = cr.getType(uri)
                ?: MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(
                        MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                            ?.lowercase()
                    )

            if (mimeType?.startsWith("image/") == true) {
                return FileCategory.IMAGE
            }

            if (isTextFile(uri, cr)) FileCategory.TEXT else FileCategory.BINARY_UNSUPPORTED
        } catch (_: Exception) {
            FileCategory.ERROR
        }
    }

    private suspend fun getDocumentFlags(uri: Uri): Int = withContext(Dispatchers.IO) {
        val projection = arrayOf(Document.COLUMN_FLAGS)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return@withContext cursor.getInt(0)
        }
        0
    }

    private inline fun <R> Uri.query(projection: String, block: (Cursor) -> R): R {
        val cursor = checkNotNull(content.query(this, arrayOf(projection), null, null, null)) {
            "the underlying content provider returned null, or it crashed."
        }
        return cursor.use(block)
    }

    private val Uri.file get() = if (scheme == "file") this.toFile() else null
}
