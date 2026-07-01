package com.klyx.terminal

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.util.Log
import android.webkit.MimeTypeMap
import com.klyx.R
import com.klyx.api.data.fs.Paths
import com.klyx.api.terminal.home
import com.klyx.data.preferences.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Collections
import java.util.LinkedList
import java.util.Locale

object SafExposureState {
    @Volatile
    var enabled: Boolean = false
}

class TerminalDocumentsProvider : DocumentsProvider() {

    companion object {
        private const val LOG_TAG = "TerminalDocumentsProvider"
        private const val ALL_MIME_TYPES = "*/*"

        private val BASE_DIR get() = Paths.home

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES
        )

        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )

        private fun authority(context: Context): String {
            return context.packageName + ".terminal.documents"
        }

        fun notifyRootsChanged(context: Context) {
            context.contentResolver.notifyChange(
                DocumentsContract.buildRootsUri(authority(context)), null
            )
        }
    }

    override fun onCreate(): Boolean {
        try {
            context?.let { ctx ->
                runBlocking {
                    ctx.dataStore.data.first()
                }.let {
                    SafExposureState.enabled = it.terminal.exposeTerminalHomeViaSaf
                }
            }
        } catch (_: Exception) {
        }
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        if (!SafExposureState.enabled) return result

        val ctx = context ?: return result
        val appName = ctx.getString(R.string.app_name)

        val row = result.newRow()
        row.add(Root.COLUMN_ROOT_ID, getDocIdForFile(BASE_DIR))
        row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        row.add(Root.COLUMN_TITLE, appName)
        row.add(
            Root.COLUMN_FLAGS,
            Root.FLAG_LOCAL_ONLY or
                    Root.FLAG_SUPPORTS_CREATE or
                    Root.FLAG_SUPPORTS_SEARCH or
                    Root.FLAG_SUPPORTS_IS_CHILD or
                    Root.FLAG_SUPPORTS_RECENTS
        )
        row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(BASE_DIR))
        row.add(Root.COLUMN_SUMMARY, null as String?)
        row.add(Root.COLUMN_MIME_TYPES, ALL_MIME_TYPES)
        row.add(Root.COLUMN_AVAILABLE_BYTES, BASE_DIR.freeSpace)
        return result
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        setNotificationUri(result)
        includeFile(result, documentId, null)
        return result
    }

    override fun queryRecentDocuments(rootId: String, projection: Array<out String>?): Cursor {
        val dir = fileForDocId(rootId)
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        setNotificationUri(result)
        val allFiles = mutableListOf<File>()
        val pending = LinkedList<File>()
        pending.add(dir)
        while (pending.isNotEmpty()) {
            val f = pending.removeFirst()
            if (f.isDirectory) {
                f.listFiles()?.let { Collections.addAll(pending, *it) }
            } else {
                allFiles.add(f)
            }
        }
        allFiles.sortByDescending { it.lastModified() }
        for (file in allFiles.take(64)) {
            includeFile(result, null, file)
        }
        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        setNotificationUri(result)
        val files = fileForDocId(parentDocumentId).listFiles()
        if (files != null) {
            for (file in files) {
                includeFile(result, null, file)
            }
        }
        return result
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val file = fileForDocId(documentId)
        val accessMode = ParcelFileDescriptor.parseMode(mode)
        return ParcelFileDescriptor.open(file, accessMode)
    }

    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point,
        signal: CancellationSignal?
    ): AssetFileDescriptor {
        val file = fileForDocId(documentId)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String
    ): String {
        var newFile = File(parentDocumentId, displayName)
        var noConflictId = 2
        while (newFile.exists()) {
            newFile = File(parentDocumentId, "$displayName ($noConflictId)")
            noConflictId++
        }
        try {
            val succeeded = if (Document.MIME_TYPE_DIR == mimeType) newFile.mkdir()
            else newFile.createNewFile()
            if (!succeeded) {
                throw FileNotFoundException("Failed to create document with id ${newFile.path}")
            }
        } catch (_: IOException) {
            throw FileNotFoundException("Failed to create document with id ${newFile.path}")
        }
        notifyFileChange()
        return newFile.path
    }

    override fun renameDocument(documentId: String, displayName: String): String {
        val oldFile = fileForDocId(documentId)
        val newFile = File(oldFile.parent, displayName)
        if (newFile.exists()) {
            throw FileNotFoundException("File already exists: $displayName")
        }
        val pathsToInvalidate = findAllPathsIn(oldFile)
        if (!oldFile.renameTo(newFile)) {
            throw FileNotFoundException("Unable to rename $documentId")
        }
        revokeDocumentsPermission(pathsToInvalidate)
        return getDocIdForFile(newFile)
    }

    override fun deleteDocument(documentId: String) {
        val file = fileForDocId(documentId)
        val allPaths = findAllPathsIn(file)
        for (path in allPaths.reversed()) {
            val f = File(path)
            if (!f.delete()) {
                throw FileNotFoundException("Cannot delete: " + f.absolutePath)
            }
            revokeDocumentPermission(getDocIdForFile(f))
        }
        notifyFileChange()
    }

    override fun getDocumentType(documentId: String): String {
        val file = fileForDocId(documentId)
        return getMimeType(file)
    }

    override fun moveDocument(
        sourceDocumentId: String,
        sourceParentDocumentId: String,
        targetParentDocumentId: String
    ): String {
        val srcFile = fileForDocId(sourceDocumentId)
        val destFile = File(fileForDocId(targetParentDocumentId), srcFile.name)
        if (destFile.exists()) {
            throw FileNotFoundException("File already exists: $destFile")
        }
        val pathsToInvalidate = findAllPathsIn(srcFile)
        if (!srcFile.renameTo(destFile)) {
            throw FileNotFoundException(
                "Cannot rename " + srcFile.absolutePath + " to " + destFile.absolutePath
            )
        }
        revokeDocumentsPermission(pathsToInvalidate)
        return getDocIdForFile(destFile)
    }

    override fun querySearchDocuments(
        rootId: String,
        query: String,
        projection: Array<out String>?
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        setNotificationUri(result)
        val parent = fileForDocId(rootId)

        val pending = LinkedList<File>()
        pending.add(parent)

        val maxSearchResults = 50
        while (pending.isNotEmpty() && result.count < maxSearchResults) {
            val file = pending.removeFirst()
            val isInsideHome = try {
                file.canonicalPath.startsWith(Paths.home.absolutePath)
            } catch (_: IOException) {
                true
            }
            if (isInsideHome) {
                if (file.isDirectory) {
                    val filesInDir = file.listFiles()
                    if (filesInDir != null) {
                        Collections.addAll(pending, *filesInDir)
                    }
                } else {
                    if (file.name.lowercase(Locale.ROOT).contains(query)) {
                        includeFile(result, null, file)
                    }
                }
            }
        }
        return result
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return documentId.startsWith(parentDocumentId)
    }

    private fun setNotificationUri(cursor: Cursor) {
        val ctx = context
        if (ctx != null) {
            val baseUri = DocumentsContract.buildChildDocumentsUri(
                authority(ctx),
                getDocIdForFile(BASE_DIR)
            )
            cursor.setNotificationUri(ctx.contentResolver, baseUri)
        }
    }

    private fun getDocIdForFile(file: File): String {
        return file.absolutePath
    }

    private fun fileForDocId(docId: String): File {
        val f = File(docId)
        if (!f.exists()) throw FileNotFoundException(f.absolutePath + " not found")
        val canonical = f.canonicalFile
        if (!canonical.absolutePath.startsWith(Paths.home.absolutePath + File.separator) &&
            canonical.absolutePath != Paths.home.absolutePath
        ) {
            throw FileNotFoundException("$docId is not inside terminal home")
        }
        return f
    }

    private fun getMimeType(file: File): String {
        if (file.isDirectory) return Document.MIME_TYPE_DIR
        val name = file.name
        val lastDot = name.lastIndexOf('.')
        if (lastDot >= 0) {
            val extension = name.substring(lastDot + 1).lowercase(Locale.ROOT)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime != null) return mime
        }
        return "application/octet-stream"
    }

    private fun includeFile(result: MatrixCursor, docId: String?, file: File?) {
        val resolvedDocId: String
        val resolvedFile: File
        if (docId == null) {
            resolvedFile = file!!
            resolvedDocId = getDocIdForFile(resolvedFile)
        } else {
            resolvedDocId = docId
            resolvedFile = fileForDocId(docId)
        }

        var flags = 0
        if (resolvedFile.isDirectory) {
            if (resolvedFile.canWrite()) {
                @Suppress("KotlinConstantConditions")
                flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
            }
        } else if (resolvedFile.canWrite()) {
            @Suppress("KotlinConstantConditions")
            flags = flags or Document.FLAG_SUPPORTS_WRITE
        }

        val parentFile = resolvedFile.parentFile
        if (parentFile != null && parentFile.canWrite()) {
            flags = flags or Document.FLAG_SUPPORTS_DELETE or
                    Document.FLAG_SUPPORTS_MOVE or
                    Document.FLAG_SUPPORTS_RENAME
        }

        val mimeType = getMimeType(resolvedFile)
        if (mimeType.startsWith("image/")) {
            flags = flags or Document.FLAG_SUPPORTS_THUMBNAIL
        }

        val row = result.newRow()
        row.add(Document.COLUMN_DOCUMENT_ID, resolvedDocId)
        row.add(Document.COLUMN_DISPLAY_NAME, resolvedFile.name)
        row.add(Document.COLUMN_SIZE, resolvedFile.length())
        row.add(Document.COLUMN_MIME_TYPE, mimeType)
        row.add(Document.COLUMN_LAST_MODIFIED, resolvedFile.lastModified())
        row.add(Document.COLUMN_FLAGS, flags)
    }

    private fun findAllPathsIn(fileOrDirectory: File): List<String> {
        val paths = mutableListOf<String>()
        val pending = LinkedList<File>()
        pending.add(fileOrDirectory)
        while (pending.isNotEmpty()) {
            val f = pending.removeFirst()
            paths.add(f.absolutePath)
            if (f.isDirectory) {
                f.listFiles()?.let { Collections.addAll(pending, *it) }
            }
        }
        return paths
    }

    private fun revokeDocumentsPermission(paths: List<String>) {
        for (path in paths) {
            Log.e(LOG_TAG, "Revoking: $path")
            revokeDocumentPermission(path)
        }
        notifyFileChange()
    }

    private fun notifyFileChange() {
        val ctx = context
        if (ctx != null) {
            val updatedUri = DocumentsContract.buildChildDocumentsUri(
                authority(ctx),
                getDocIdForFile(BASE_DIR)
            )
            ctx.contentResolver.notifyChange(updatedUri, null)
        }
    }
}
