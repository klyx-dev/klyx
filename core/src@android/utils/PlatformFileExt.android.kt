package com.klyx.core.utils

import android.net.Uri
import android.provider.DocumentsContract
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile
import java.io.File

actual fun PlatformFile.isTermuxUri(): Boolean =
    (androidFile as? AndroidFile.UriWrapper)?.uri?.authority == "com.termux.documents"

actual fun PlatformFile.actualUri(): String {
    return when (val file = androidFile) {
        is AndroidFile.FileWrapper -> Uri.fromFile(file.file).toString()
        is AndroidFile.UriWrapper -> {
            if (isTermuxUri()) {
                val docId = if (DocumentsContract.isTreeUri(file.uri)) {
                    DocumentsContract.getTreeDocumentId(file.uri)
                } else {
                    DocumentsContract.getDocumentId(file.uri)
                }

                Uri.fromFile(File(docId)).toString()
            } else {
                file.uri.toString()
            }
        }
    }
}

actual fun PlatformFile.isFileUri() = androidFile is AndroidFile.FileWrapper

