package com.klyx.terminal

import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.klyx.R
import com.klyx.core.Environment
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.time.Duration.Companion.days

class TerminalDocumentsProvider : DocumentsProvider() {
    private val home get() = with(context!!) { userHomeDir }

    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor? {
        val file = getFileForDocumentId(documentId ?: return null)

        if (!file.exists()) {
            throw FileNotFoundException("File not found: ${file.absolutePath}")
        }

        val accessMode = ParcelFileDescriptor.parseMode(mode ?: "r")
        return ParcelFileDescriptor.open(file, accessMode)
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocumentId(parentDocumentId ?: return null)
        if (!parent.isDirectory) return null

        val files = parent.listFiles() ?: return result
        for (file in files) {
            val childDocumentId = getDocumentIdForFile(file)
            includeFile(result, file, childDocumentId)
        }

        return result
    }

    override fun queryDocument(
        documentId: String?,
        projection: Array<out String>?
    ): Cursor? {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val file = getFileForDocumentId(documentId ?: return null)
        if (!file.exists()) return null
        includeFile(result, file, documentId)
        return result
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)

        result.newRow().apply {
            add(Root.COLUMN_ROOT_ID, ROOT_ID)
            add(Root.COLUMN_MIME_TYPES, "*/*")
            add(
                Root.COLUMN_FLAGS,
                Root.FLAG_SUPPORTS_CREATE or Root.FLAG_SUPPORTS_RECENTS or Root.FLAG_SUPPORTS_SEARCH or Root.FLAG_SUPPORTS_IS_CHILD
            )
            add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
            add(Root.COLUMN_TITLE, Environment.AppName)
            add(Root.COLUMN_SUMMARY, null)
            add(Root.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID)
            add(Root.COLUMN_AVAILABLE_BYTES, home!!.freeSpace)
        }

        return result
    }

    override fun onCreate(): Boolean {
        return home != null
    }

    private fun getFileForDocumentId(documentId: String): File {
        return when (documentId) {
            ROOT_DOCUMENT_ID -> home!!
            else -> {
                val relativePath = documentId.removePrefix("${ROOT_ID}_")
                File(home, relativePath)
            }
        }
    }

    private fun getDocumentIdForFile(file: File): String {
        return when {
            file.absolutePath == home!!.absolutePath -> ROOT_DOCUMENT_ID
            else -> {
                val relativePath = file.absolutePath.removePrefix(home!!.absolutePath).removePrefix("/")
                "${ROOT_ID}_$relativePath"
            }
        }
    }

    private fun includeFile(cursor: MatrixCursor, file: File, documentId: String) {
        var flags = 0

        if (file.canWrite()) {
            if (file.isDirectory) {
                flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
            } else {
                flags = flags or Document.FLAG_SUPPORTS_WRITE
            }
            flags = flags or Document.FLAG_SUPPORTS_DELETE
            flags = flags or Document.FLAG_SUPPORTS_RENAME
        }

        val mimeType = if (file.isDirectory) {
            Document.MIME_TYPE_DIR
        } else {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
        }

        cursor.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, documentId)
            add(Document.COLUMN_MIME_TYPE, mimeType)
            add(Document.COLUMN_DISPLAY_NAME, file.name)
            add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
            add(Document.COLUMN_FLAGS, flags)
            add(Document.COLUMN_SIZE, file.length())
        }
    }

    override fun createDocument(parentDocumentId: String?, mimeType: String?, displayName: String?): String? {
        val parent = getFileForDocumentId(parentDocumentId ?: return null)
        if (!parent.isDirectory) return null

        var newFile = File(parent, displayName ?: "untitled")
        var id = 1
        while (newFile.exists()) {
            newFile = File(parent, "${displayName ?: "untitled"} (${id++})")
        }

        try {
            val success = if (mimeType == Document.MIME_TYPE_DIR) {
                newFile.mkdirs()
            } else {
                newFile.createNewFile()
            }

            if (success) {
                return getDocumentIdForFile(newFile)
            }
        } catch (e: Exception) {
            throw IOException("Failed to create document: ${e.message}", e)
        }

        return null
    }

    override fun deleteDocument(documentId: String?) {
        val file = getFileForDocumentId(documentId ?: return)

        if (!file.exists()) {
            throw FileNotFoundException("Cannot delete: file not found ${file.absolutePath}")
        }

        val success = if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }

        if (!success) {
            throw IOException("Failed to delete document: ${file.absolutePath}")
        }
    }

    override fun renameDocument(documentId: String?, displayName: String?): String? {
        val file = getFileForDocumentId(documentId ?: return null)
        if (!file.exists() || displayName.isNullOrBlank()) return null

        val newFile = File(file.parent, displayName)
        return if (file.renameTo(newFile)) {
            getDocumentIdForFile(newFile)
        } else {
            null
        }
    }

    override fun getDocumentType(documentId: String?): String? {
        val file = getFileForDocumentId(documentId ?: return null)
        if (!file.exists()) return null

        return if (file.isDirectory) {
            Document.MIME_TYPE_DIR
        } else {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
        }
    }

    override fun querySearchDocuments(rootId: String?, query: String?, projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        if (query.isNullOrBlank()) return result
        searchFiles(home!!, query, result)
        return result
    }

    private fun searchFiles(directory: File, query: String, cursor: MatrixCursor) {
        val files = directory.listFiles() ?: return

        for (file in files) {
            if (file.name.contains(query, ignoreCase = true)) {
                val documentId = getDocumentIdForFile(file)
                includeFile(cursor, file, documentId)
            }

            if (file.isDirectory) {
                searchFiles(file, query, cursor)
            }
        }
    }

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean {
        val parent = getFileForDocumentId(parentDocumentId ?: return false)
        val child = getFileForDocumentId(documentId ?: return false)
        if (!parent.isDirectory || !child.exists()) return false

        var currentParent = child.parentFile
        while (currentParent != null) {
            if (currentParent.absolutePath == parent.absolutePath) {
                return true
            }
            currentParent = currentParent.parentFile
        }
        return false
    }

    override fun openDocumentThumbnail(
        documentId: String?,
        sizeHint: Point?,
        signal: CancellationSignal?
    ): AssetFileDescriptor? {
        val file = getFileForDocumentId(documentId ?: return null)
        if (!file.exists()) return null

        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
        if (mimeType?.startsWith("image/") == false) return null

        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            AssetFileDescriptor(pfd, 0, pfd.statSize)
        } catch (_: Exception) {
            null
        }
    }

    override fun copyDocument(sourceDocumentId: String?, targetParentDocumentId: String?): String? {
        val sourceFile = getFileForDocumentId(sourceDocumentId ?: return null)
        val targetParent = getFileForDocumentId(targetParentDocumentId ?: return null)

        if (!sourceFile.exists() || !targetParent.isDirectory) return null

        val targetFile = File(targetParent, sourceFile.name)
        return try {
            if (sourceFile.isDirectory) {
                sourceFile.copyRecursively(targetFile)
            } else {
                sourceFile.copyTo(targetFile)
            }
            getDocumentIdForFile(targetFile)
        } catch (_: Exception) {
            null
        }
    }

    override fun moveDocument(
        sourceDocumentId: String?,
        sourceParentDocumentId: String?,
        targetParentDocumentId: String?
    ): String? {
        val sourceFile = getFileForDocumentId(sourceDocumentId ?: return null)
        val targetParent = getFileForDocumentId(targetParentDocumentId ?: return null)
        if (!sourceFile.exists() || !targetParent.isDirectory) return null

        val targetFile = File(targetParent, sourceFile.name)
        return if (sourceFile.renameTo(targetFile)) {
            getDocumentIdForFile(targetFile)
        } else {
            null
        }
    }

    override fun removeDocument(documentId: String?, parentDocumentId: String?) {
        deleteDocument(documentId)
    }

    override fun queryRecentDocuments(rootId: String?, projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val recentFiles = mutableListOf<File>()
        collectRecentFiles(home!!, recentFiles)

        recentFiles.sortByDescending { it.lastModified() }
        recentFiles.take(50).forEach { file ->
            val documentId = getDocumentIdForFile(file)
            includeFile(result, file, documentId)
        }

        return result
    }

    private fun collectRecentFiles(directory: File, recentFiles: MutableList<File>) {
        val files = directory.listFiles() ?: return
        val cutoffTime = System.currentTimeMillis() - 30.days.inWholeMilliseconds

        for (file in files) {
            if (file.isFile && file.lastModified() > cutoffTime) {
                recentFiles.add(file)
            } else if (file.isDirectory) {
                collectRecentFiles(file, recentFiles)
            }
        }
    }

    companion object {
        private const val ROOT_ID = "klyx_terminal_home"
        private const val ROOT_DOCUMENT_ID = "klyx_terminal_home_root"

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
    }
}
