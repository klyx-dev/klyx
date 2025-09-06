package com.klyx.core.terminal

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

object SAFUtils : KoinComponent {
    private val context: Context by inject()
    private val terminalPrefs = context.getSharedPreferences("terminal", Context.MODE_PRIVATE)

    private val currentUser get() = terminalPrefs.getString("currentUser", null)
    private val ubuntuDir get() = File(context.filesDir, "ubuntu")
    private val ubuntuHomeDir get() = ubuntuDir.resolve("home")
    private val userHomeDir get() = if (currentUser == null) null else ubuntuHomeDir.resolve(currentUser!!)

    const val ROOT_ID = "klyx_terminal_home"
    const val ROOT_DOCUMENT_ID = "klyx_terminal_home_root"

    fun getDocumentIdForUri(uri: String) = getDocumentIdForUri(uri.toUri())
    fun getDocumentIdForUri(uri: Uri): String = DocumentsContract.getDocumentId(uri)

    fun getFileForDocumentId(documentId: String): File {
        requireNotNull(userHomeDir) { "No user home directory" }

        return when (documentId) {
            ROOT_DOCUMENT_ID -> userHomeDir!!
            else -> {
                val relativePath = documentId.removePrefix("${ROOT_ID}_")
                File(userHomeDir, relativePath)
            }
        }
    }

    fun getDocumentIdForFile(file: File): String {
        requireNotNull(userHomeDir) { "No user home directory" }

        return when {
            file.absolutePath == userHomeDir!!.absolutePath -> ROOT_DOCUMENT_ID
            else -> {
                val relativePath = file.absolutePath.removePrefix(userHomeDir!!.absolutePath).removePrefix("/")
                "${ROOT_ID}_$relativePath"
            }
        }
    }
}
