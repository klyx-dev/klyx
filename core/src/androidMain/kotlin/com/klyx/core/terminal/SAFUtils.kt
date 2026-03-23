package com.klyx.core.terminal

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.klyx.core.io.Paths
import com.klyx.core.io.fs
import com.klyx.core.io.root
import java.io.File

object SAFUtils {
    const val ROOT_ID = "klyx_terminal_home"
    const val ROOT_DOCUMENT_ID = "klyx_terminal_home_root"

    fun getDocumentIdForUri(uri: String) = getDocumentIdForUri(uri.toUri())
    fun getDocumentIdForUri(uri: Uri): String = DocumentsContract.getDocumentId(uri)

    fun getFileForDocumentId(documentId: String): File {
        require(fs.exists(Paths.root)) { "unpossible" }

        return when (documentId) {
            ROOT_DOCUMENT_ID -> File(Paths.root.toString())
            else -> {
                val relativePath = documentId.removePrefix("${ROOT_ID}_")
                File(Paths.root.toString(), relativePath)
            }
        }
    }

    fun getDocumentIdForFile(file: File): String {
        require(fs.exists(Paths.root)) { "unpossible" }

        return when {
            file.absolutePath == Paths.root.toString() -> ROOT_DOCUMENT_ID
            else -> {
                val relativePath = file.absolutePath.removePrefix(Paths.root.toString()).removePrefix("/")
                "${ROOT_ID}_$relativePath"
            }
        }
    }
}
