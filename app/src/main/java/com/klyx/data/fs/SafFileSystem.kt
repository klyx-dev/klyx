package com.klyx.data.fs

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Document.MIME_TYPE_DIR
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.klyx.api.data.file.KxFile
import com.klyx.api.data.file.wrap
import com.klyx.api.data.fs.FileCapabilities
import com.klyx.api.data.fs.FileCategory
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.util.isTextFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger

class SafFileSystem(
    private val context: Context,
) : FileSystem {

    private val content = context.contentResolver

    private val providerAuthority = "${context.packageName}.terminal.documents"

    override suspend fun list(uri: Uri): List<KxFile> = withContext(Dispatchers.IO) {
        val localFile = resolveToLocalFile(uri)
        if (localFile != null) {
            return@withContext localFile.listFiles()?.map { it.wrap() }.orEmpty()
        }

        if (DocumentsContract.isTreeUri(uri)) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val parts = uri.encodedPath?.split("/") ?: emptyList()
                val rootTreeUri = if (parts.size >= 3) {
                    uri.buildUpon().encodedPath("/tree/${parts[2]}").build()
                } else {
                    uri
                }
                listSafBatched(rootTreeUri, docId)
            } else {
                listSafBatched(uri)
            }
        } else {
            DocumentFile.fromSingleUri(context, uri)?.listFiles()?.map { it.toKxFile() }.orEmpty()
        }
    }

    private fun resolveToLocalFile(uri: Uri): File? {
        if (uri.authority != providerAuthority) return null
        return when {
            DocumentsContract.isTreeUri(uri) -> File(DocumentsContract.getTreeDocumentId(uri))
            DocumentsContract.isDocumentUri(context, uri) -> File(DocumentsContract.getDocumentId(uri))
            else -> null
        }
    }

    private fun listSafBatched(treeUri: Uri, parentDocId: String? = null): List<KxFile> {
        val docId = parentDocId ?: DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
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

            val results = mutableListOf<KxFile>()
            while (c.moveToNext()) {
                val docId = c.getString(idIdx)
                val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                val name = if (nameIdx >= 0) c.getString(nameIdx) ?: docId else docId
                results.add(
                    KxFile(
                        uriString = childUri.toString(),
                        name = name,
                        isDirectory = MIME_TYPE_DIR == if (mimeIdx >= 0) c.getString(mimeIdx) else null,
                        size = if (sizeIdx >= 0) c.getLong(sizeIdx) else 0L,
                        lastModified = if (dateIdx >= 0) c.getLong(dateIdx) else 0L,
                    )
                )
            }
            return results
        }

        return DocumentFile.fromTreeUri(context, treeUri)?.listFiles()?.map { it.toKxFile() }.orEmpty()
    }

    override suspend fun search(
        roots: List<Uri>,
        query: String,
        maxResults: Int
    ): Flow<KxFile> = channelFlow {
        if (query.isBlank()) return@channelFlow
        val queryLower = query.lowercase()
        val perRootMax = maxResults / roots.size.coerceAtLeast(1) + 1
        val globalCount = AtomicInteger(0)

        val onResult: (KxFile) -> Unit = { file ->
            if (globalCount.getAndIncrement() < maxResults) {
                trySend(file)
            }
        }

        for (root in roots) {
            if (globalCount.get() >= maxResults) break
            val localFile = resolveToLocalFile(root)
            if (localFile != null) {
                searchLocalSaf(localFile, queryLower, perRootMax, onResult)
            } else if (DocumentsContract.isTreeUri(root)) {
                searchSaf(root, queryLower, perRootMax, onResult)
            }
        }
    }

    private suspend fun searchLocalSaf(
        root: File,
        query: String,
        maxResults: Int,
        onResult: (KxFile) -> Unit
    ) {
        try {
            val topLevel = root.listFiles() ?: return
            val count = AtomicInteger(0)
            for (entry in topLevel) {
                if (count.get() >= maxResults) break
                if (entry.name.lowercase().contains(query)) {
                    if (count.getAndIncrement() < maxResults) {
                        onResult(entry.wrap())
                    }
                }
                if (entry.isDirectory) {
                    searchLocalSaf(entry, query, maxResults, onResult)
                }
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun searchSaf(
        treeUri: Uri,
        query: String,
        maxResults: Int,
        onResult: (KxFile) -> Unit
    ) = withContext(Dispatchers.IO) {
        val columns = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED
        )

        val queue = ArrayDeque<String>()
        queue.add(DocumentsContract.getTreeDocumentId(treeUri))
        val seen = mutableSetOf<String>()
        val safCount = AtomicInteger(0)

        while (queue.isNotEmpty() && safCount.get() < maxResults) {
            val dirDocId = queue.removeFirst()
            if (!seen.add(dirDocId)) continue
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, dirDocId)

            try {
                val cursor = content.query(childrenUri, columns, null, null, null) ?: continue
                cursor.use { c ->
                    val idIdx = c.getColumnIndex(Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = c.getColumnIndex(Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = c.getColumnIndex(Document.COLUMN_MIME_TYPE)
                    val sizeIdx = c.getColumnIndex(Document.COLUMN_SIZE)
                    val dateIdx = c.getColumnIndex(Document.COLUMN_LAST_MODIFIED)

                    while (c.moveToNext() && safCount.get() < maxResults) {
                        val docId = c.getString(idIdx)
                        val name = if (nameIdx >= 0) c.getString(nameIdx) ?: docId else docId
                        val mimeType = if (mimeIdx >= 0) c.getString(mimeIdx) else null

                        if (MIME_TYPE_DIR == mimeType) {
                            queue.add(docId)
                        }

                        if (name.lowercase().contains(query)) {
                            if (safCount.getAndIncrement() < maxResults) {
                                val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                onResult(
                                    KxFile(
                                        uriString = childUri.toString(),
                                        name = name,
                                        isDirectory = MIME_TYPE_DIR == mimeType,
                                        size = if (sizeIdx >= 0) c.getLong(sizeIdx) else 0L,
                                        lastModified = if (dateIdx >= 0) c.getLong(dateIdx) else 0L,
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun inputStream(uri: Uri): InputStream = withContext(Dispatchers.IO) {
        checkNotNull(content.openInputStream(uri)) { "the provider recently crashed." }
    }

    override suspend fun outputStream(uri: Uri, mode: String): OutputStream = withContext(Dispatchers.IO) {
        checkNotNull(content.openOutputStream(uri, mode)) { "the provider recently crashed." }
    }

    override suspend fun delete(uri: Uri) = withContext(Dispatchers.IO) {
        DocumentsContract.deleteDocument(content, uri)
    }

    override suspend fun rename(uri: Uri, newName: String): Uri? = withContext(Dispatchers.IO) {
        DocumentsContract.renameDocument(content, uri, newName)
    }

    override suspend fun createFile(parent: Uri, name: String, mimeType: String): Uri? = withContext(Dispatchers.IO) {
        DocumentsContract.createDocument(content, parent, mimeType, name)
    }

    override suspend fun createDirectory(parent: Uri, name: String): Uri? = withContext(Dispatchers.IO) {
        createFile(parent, name, MIME_TYPE_DIR)
    }

    override suspend fun capabilities(uri: Uri): FileCapabilities = withContext(Dispatchers.IO) {
        val flags = getDocumentFlags(uri)
        FileCapabilities(
            canWrite = flags and Document.FLAG_SUPPORTS_WRITE != 0,
            canDelete = flags and Document.FLAG_SUPPORTS_DELETE != 0,
            canRename = flags and Document.FLAG_SUPPORTS_RENAME != 0,
            canCreate = flags and Document.FLAG_DIR_SUPPORTS_CREATE != 0
        )
    }

    override suspend fun fileName(uri: Uri): String? = withContext(Dispatchers.IO) {
        uri.query(OpenableColumns.DISPLAY_NAME) { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1 && cursor.moveToFirst()) {
                cursor.getString(index)
            } else uri.lastPathSegment
        }
    }

    override suspend fun exists(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        content.query(
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
        try {
            DocumentsContract.copyDocument(content, source, targetParent)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun move(
        source: Uri,
        sourceParent: Uri,
        targetParent: Uri
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.moveDocument(content, source, sourceParent, targetParent)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun wrapUri(uri: Uri): KxFile = withContext(Dispatchers.IO) {
        val isDirectory = DocumentsContract.isTreeUri(uri) ||
            (DocumentsContract.isDocumentUri(context, uri) &&
                MIME_TYPE_DIR == context.contentResolver.getType(uri))

        val name = if (DocumentsContract.isTreeUri(uri)) {
            DocumentsContract.getTreeDocumentId(uri).substringAfterLast('/')
                .substringAfterLast('%')
        } else {
            uri.lastPathSegment
        } ?: uri.toString()

        KxFile(
            uriString = uri.toString(),
            name = name,
            isDirectory = isDirectory
        )
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

    override suspend fun mimeType(uri: Uri): String? = withContext(Dispatchers.IO) {
        val cr = context.contentResolver
        cr.getType(uri)
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(uri.toString())?.lowercase()
                )
    }

    private suspend fun getDocumentFlags(uri: Uri): Int = withContext(Dispatchers.IO) {
        val projection = arrayOf(Document.COLUMN_FLAGS)
        content.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return@withContext cursor.getInt(0)
        }
        0
    }

    private fun DocumentFile.toKxFile() = KxFile(
        uriString = uri.toString(),
        name = name ?: uri.lastPathSegment ?: "",
        isDirectory = isDirectory,
        size = length(),
        lastModified = lastModified(),
    )

    private inline fun <R> Uri.query(projection: String, block: (Cursor) -> R): R {
        val cursor = checkNotNull(content.query(this, arrayOf(projection), null, null, null)) {
            "the underlying content provider returned null, or it crashed."
        }
        return cursor.use(block)
    }
}
