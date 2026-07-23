package com.klyx.data.file

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.klyx.api.data.file.KxFile
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.util.applicationContext
import com.klyx.api.util.withApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File

val KxFile.shareableUri: Uri
    get() = if (uri.scheme == "file") {
        val context = applicationContext()
        FileProvider.getUriForFile(context, "${context.packageName}.provider", File(uri.path!!))
    } else uri

private fun KxFile.localShareableUri(): Uri {
    if (uri.scheme == "file") return shareableUri
    val fileSystem: FileSystem = org.koin.core.context.GlobalContext.get().get()
    val tempDir = File(applicationContext().cacheDir, "share")
    tempDir.mkdirs()
    val tempFile = File(tempDir, name)
    runBlocking(Dispatchers.IO) {
        fileSystem.inputStream(uri).use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
    return FileProvider.getUriForFile(applicationContext(), "${applicationContext().packageName}.provider", tempFile)
}

fun KxFile.mimeType() = when (extension.lowercase()) {
    "xml" -> "text/xml"
    "json" -> "application/json"
    "md" -> "text/markdown"
    else -> MimeTypeMap
        .getSingleton()
        .getMimeTypeFromExtension(extension.lowercase())
}

fun KxFile.openWith() = withApplicationContext {
    if (isDirectory) return@withApplicationContext

    try {
        val intentUri = localShareableUri()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(intentUri, mimeType() ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(
            Intent.createChooser(intent, "Open with")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            applicationContext,
            "No application found to open this file",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        Toast.makeText(
            applicationContext,
            "Could not open file: ${e.localizedMessage}",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun KxFile.share() = withApplicationContext {
    if (isDirectory) return@withApplicationContext

    try {
        val intentUri = localShareableUri()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType() ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, intentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(
            Intent
                .createChooser(intent, "Share file")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            applicationContext,
            "No application available for sharing",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        Toast.makeText(
            applicationContext,
            "Could not share file: ${e.localizedMessage}",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun KxFile.resolveName(): String {
    val path = uri.path ?: return name
    val context = applicationContext()
    return when (path) {
        Environment.getExternalStorageDirectory().absolutePath -> "Internal Storage"
        context.dataDir.absolutePath -> "App Data"
        context.filesDir.resolve("home").absolutePath,
        context.filesDir.resolve("home").canonicalPath,
            -> "Terminal Home"

        else -> name
    }
}
