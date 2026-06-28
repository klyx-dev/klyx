package com.klyx.presentation.components.filetree

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.klyx.api.data.file.KxFile
import com.klyx.api.data.file.resolveName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class FileNode(
    val file: KxFile,
    val name: String = file.resolveName(),
    val isDirectory: Boolean = file.isDirectory
) : java.io.Serializable {

    val uri get() = file.uri

    val parent by lazy { file.parentFile?.let(::FileNode) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileNode

        return file.uri == other.file.uri
    }

    override fun hashCode() = file.uri.hashCode()
}

@Immutable
data class FlatNode(
    val node: FileNode,
    val depth: Int,
    val rootNode: FileNode
)

fun FileNode(uri: Uri) = FileNode(KxFile(uri))
