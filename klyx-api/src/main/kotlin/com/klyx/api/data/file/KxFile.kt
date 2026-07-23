package com.klyx.api.data.file

import android.net.Uri
import kotlinx.serialization.Serializable
import java.io.File
import java.io.Serializable as JavaSerializable

@Serializable
class KxFile(
    private val uriString: String,
    val name: String,
    val isDirectory: Boolean = false,
    val size: Long = 0L,
    val lastModified: Long = 0L,
) : JavaSerializable {

    val uri: Uri get() = Uri.parse(uriString)

    val extension: String
        get() = when {
            name.startsWith(".") && name.count { it == '.' } == 1 -> ""
            name.contains(".") -> name.substringAfterLast(".")
            else -> ""
        }

    val isHidden: Boolean get() = name.startsWith(".")

    val parent: KxFile?
        get() {
            val path = uri.path?.trimEnd('/') ?: return null
            val parentPath = path.substringBeforeLast('/')
            if (parentPath.isEmpty() || parentPath == path) return null
            return KxFile(uri.buildUpon().path(parentPath).build())
        }

    override fun toString(): String = uri.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KxFile) return false
        return uri == other.uri
    }

    override fun hashCode(): Int = uri.hashCode()
}

fun KxFile(uri: Uri): KxFile = KxFile(
    uriString = uri.toString(),
    name = uri.lastPathSegment ?: uri.toString(),
)

fun File.wrap(): KxFile = KxFile(
    uriString = Uri.fromFile(this).toString(),
    name = name,
    isDirectory = isDirectory,
    size = length(),
    lastModified = lastModified(),
)

fun Uri.wrap(): KxFile = KxFile(this)

val KxFile.providerKey: String get() = extension.ifBlank { name.lowercase() }
