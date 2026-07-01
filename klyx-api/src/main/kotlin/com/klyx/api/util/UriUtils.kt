package com.klyx.api.util

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Returns true if this [Uri] uses the `file` scheme.
 */
val Uri.isFileUri get() = scheme == ContentResolver.SCHEME_FILE

/**
 * Decodes a URL-encoded string. It performs two passes of decoding to handle nested encoding.
 */
fun String.decodeEscaped(): String = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        URLDecoder.decode(URLDecoder.decode(this, Charsets.UTF_8), Charsets.UTF_8)
    } else {
        @Suppress("DEPRECATION")
        URLDecoder.decode(URLDecoder.decode(this))
    }
}.getOrDefault(this)

/**
 * URL-encodes a string using UTF-8.
 */
fun String.encodeEscaped(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        URLEncoder.encode(this, Charsets.UTF_8)
    } else {
        @Suppress("DEPRECATION")
        URLEncoder.encode(this)
    }
}

/**
 * Converts a `file://` URI into a shareable `content://` URI using [FileProvider].
 * If the URI is already a content URI, it is returned as-is.
 */
val Uri.shareableUri: Uri
    get() = if (scheme == "file") {
        val context = applicationContext()
        FileProvider.getUriForFile(context, "${context.packageName}.provider", this.toFile())
    } else this

/**
 * Shares the content represented by this [Uri] using the Android share sheet.
 */
fun Uri.share() = withApplicationContext {
    try {
        val mimeType = contentResolver.getType(this@share)
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(this.toString())
                        ?.lowercase()
                )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, shareableUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(
            Intent
                .createChooser(intent, "Share via")
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
            "Could not share uri: ${e.localizedMessage}",
            Toast.LENGTH_LONG
        ).show()
    }
}

