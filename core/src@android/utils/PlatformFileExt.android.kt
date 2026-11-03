package com.klyx.core.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.klyx.core.App
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

actual suspend fun PlatformFile.tryGetActualFile() = withContext(Dispatchers.IO) {
    (androidFile as? AndroidFile.UriWrapper)?.uri?.tryGetFile()?.let(::PlatformFile)
}

private fun Uri.tryGetFile(): File? {
    if (scheme == "file" && path != null) return File(path!!)

    if (path != null) {
        val externals = arrayOf("/external/", "/external_path/")
        var file: File?
        for (external in externals) {
            if (path!!.startsWith(external)) {
                file = File(Environment.getExternalStorageDirectory().absolutePath + path!!.replace(external, "/"))
                if (file.exists()) return file
            }
        }
    }

    if (DocumentsContract.isTreeUri(this)) return tryGetTreeFile()

    val context: Context = App.global.get()
    if (DocumentsContract.isDocumentUri(context, this)) {
        when (authority) {
            "com.android.externalstorage.documents" -> {
                val docId = DocumentsContract.getDocumentId(this)
                val split = docId.split(":")
                val type = split[0]
                if (type.equals("primary", ignoreCase = true)) {
                    return File(Environment.getExternalStorageDirectory().absolutePath + "/" + split[1])
                } else {
                    val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                    try {
                        for (volume in storageManager.storageVolumes) {
                            val mounted =
                                volume.state == Environment.MEDIA_MOUNTED || volume.state == Environment.MEDIA_MOUNTED_READ_ONLY
                            //if the media is not mounted, we need not get the volume details
                            if (!mounted) continue
                            //Primary storage is already handled.
                            if (volume.isPrimary && volume.isEmulated) continue

                            if (volume.uuid == type) {
                                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    File(volume.directory!!.absolutePath + "/" + split[1])
                                } else {
                                    val getPath = StorageVolume::class.java.getMethod("getPath")
                                    val path = getPath(volume) as String
                                    File(path + "/" + split[1])
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PlatformFileExt", "Error getting file from storage volume", e)
                    }
                }
            }

            "com.android.providers.downloads.documents" -> {
                var id = DocumentsContract.getDocumentId(this)
                if (id.isNullOrEmpty()) return null

                if (id.startsWith("raw:")) {
                    return File(id.substring(4))
                } else if (id.startsWith("msf:")) {
                    id = id.split(":")[1]
                }

                val availableId = id.toLongOrNull() ?: return null
                val contentUriPrefixesToTry = arrayOf(
                    "content://downloads/public_downloads",
                    "content://downloads/all_downloads",
                    "content://downloads/my_downloads"
                )

                for (contentUriPrefix in contentUriPrefixesToTry) {
                    val contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), availableId)
                    try {
                        val file = context(context) { fileFromUri(contentUri) }
                        if (file != null) return file
                    } catch (_: Exception) {
                        //ignore
                    }
                }
            }

            "com.android.providers.media.documents" -> {
                val docId = DocumentsContract.getDocumentId(this)
                val split = docId.split(":")
                val type = split[0]
                val contentUri = when (type) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> null
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                return context(context) { fileFromUri(contentUri, selection, selectionArgs) }
            }

            else -> {
                return if (scheme == "content") {
                    context(context) { fileFromUri(this) }
                } else {
                    null
                }
            }
        }
    } else if (scheme == "content") {
        return context(context) { fileFromUri(this) }
    }
    return null
}

private fun Uri.tryGetTreeFile(): File? {
    if (!DocumentsContract.isTreeUri(this)) return null

    if (authority != "com.android.externalstorage.documents") {
        return null
    }

    val treeId = DocumentsContract.getTreeDocumentId(this)
    val split = treeId.split(":", limit = 2)

    if (split.size != 2) return null

    val storageType = split[0]
    val relativePath = split[1]

    if (!storageType.equals("primary", ignoreCase = true)) {
        return null
    }

    return File(
        Environment.getExternalStorageDirectory(),
        relativePath
    ).takeIf { it.isDirectory }
}

context(context: Context)
private fun fileFromUri(
    uri: Uri?,
    selection: String? = null,
    selectionArgs: Array<String>? = null
): File? {
    if (uri == null) return null

    when (uri.authority) {
        "com.google.android.apps.photos.content" -> {
            if (!uri.lastPathSegment.isNullOrEmpty()) return File(uri.lastPathSegment!!)
        }

        "com.tencent.mtt.fileprovider" -> {
            val path = uri.path
            if (!path.isNullOrEmpty()) {
                val filesDir = Environment.getExternalStorageDirectory()
                return File(filesDir, path.substring("/QQBrowser".length, path.length))
            }
        }

        "com.huawei.hidisk.fileprovider" -> {
            val path = uri.path
            if (!path.isNullOrEmpty()) return File(path.replace("/root", ""))
        }
    }

    val cursor = context.contentResolver.query(uri, arrayOf("_data"), selection, selectionArgs, null) ?: return null
    return cursor.use { cursor ->
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex("_data")
            if (columnIndex >= 0) {
                File(cursor.getString(columnIndex))
            } else null
        } else null
    }
}
