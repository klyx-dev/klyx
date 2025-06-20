package com.klyx.core.file

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import com.klyx.ifNull

class AndroidFileWrapper(
    private val context: Context,
    private val raw: DocumentFile
) : FileWrapper {
    private val _name by lazy { raw.name ?: "(unknown)" }
    private val _isDirectory by lazy { raw.isDirectory }
    private val _isFile by lazy { raw.isFile }

    override val absolutePath get() = raw.uri.toString()
    override val canonicalPath get() = raw.uri.toString()
    override val name get() = _name
    override val path get() = raw.uri.path ?: "UNKNOWN"
    override val mimeType get() = raw.type
    override val parent get() = raw.parentFile?.uri?.toString()
    override val parentFile get() = raw.parentFile?.let { AndroidFileWrapper(context, it) }
    override val isFile get() = _isFile
    override val isDirectory get() = _isDirectory
    override val canRestoreFromPath get() = false
    override val id get() = "$name:$length:$lastModified"
    override val length get() = raw.length()
    override val lastModified get() = raw.lastModified()

    override fun canRead() = raw.canRead()
    override fun canWrite() = raw.canWrite()
    override fun exists() = raw.exists()

    override fun list() = raw.listFiles().mapNotNull { it.name }.toTypedArray()
    override fun listFiles() = raw.listFiles().mapNotNull { AndroidFileWrapper(context, it) }

    override fun listFiles(filter: (FileWrapper) -> Boolean): List<AndroidFileWrapper> {
        return raw.listFiles().mapNotNull { AndroidFileWrapper(context, it) }.filter(filter)
    }

    override fun listFiles(filter: (FileWrapper) -> Boolean, recursive: Boolean): List<FileWrapper>? {
        return raw.listFiles().mapNotNull { AndroidFileWrapper(context, it) }.filter(filter).let { files ->
            if (recursive) {
                files.flatMap {
                    @Suppress("KotlinConstantConditions")
                    it.listFiles(filter, recursive) ?: emptyList()
                }
            } else {
                files
            }
        }
    }

    override fun readText() = inputStream()?.bufferedReader()?.use { it.readText() }.ifNull { "" }

    override fun writeText(text: String) = try {
        outputStream()!!.bufferedWriter().use { it.write(text) }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    fun uri() = raw.uri
    fun inputStream() = context.contentResolver.openInputStream(raw.uri)
    fun outputStream() = context.contentResolver.openOutputStream(raw.uri)

    fun isFromTermux() = raw.uri.host == "com.termux.documents"

    companion object {
        fun shouldWrap(uri: Uri) = ContentResolver.SCHEME_CONTENT == uri.scheme && "com.termux.documents" == uri.host
    }
}


fun AndroidFileWrapper.requiresPermission(context: Context, isWrite: Boolean): Boolean {
    // External storage root check
    val externalDirs = context.getExternalFilesDirs(null).mapNotNull { it?.parentFile?.parentFile?.parentFile }
    val isExternalStorage = externalDirs.any { this.absolutePath.startsWith(it.absolutePath) }

    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            // For Android 11 and above (Scoped Storage)
            // Apps can access their own app-specific dirs freely
            !this.isInAppSpecificDir(context) && !this.canAccess(context)
        }

        isExternalStorage -> {
            val permission = if (isWrite) Manifest.permission.WRITE_EXTERNAL_STORAGE else Manifest.permission.READ_EXTERNAL_STORAGE
            context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }

        else -> false // Internal storage or app-private paths generally don't require extra permission
    }
}

private fun AndroidFileWrapper.isInAppSpecificDir(context: Context): Boolean {
    val appSpecificDirs = listOfNotNull(
        context.filesDir,
        context.cacheDir,
        context.externalCacheDir,
        context.getExternalFilesDir(null)
    ).map { it.absolutePath }

    return appSpecificDirs.any { this.absolutePath.startsWith(it) }
}

private fun AndroidFileWrapper.canAccess(context: Context): Boolean {
    return if (exists()) {
        if (canRead() && canWrite()) true
        else try {
            if (isDirectory) list().isNotEmpty()
            else inputStream()?.close() != null
        } catch (e: Exception) {
            false
        }
    } else {
        val parent = this.parentFile ?: return false
        parent.canWrite()
    }
}
