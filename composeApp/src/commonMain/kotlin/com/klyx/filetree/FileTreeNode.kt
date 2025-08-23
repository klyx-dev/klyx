package com.klyx.filetree

import com.klyx.core.file.KxFile
import com.klyx.core.file.resolveName
import com.klyx.core.file.toKxFile
import io.github.vinceglb.filekit.PlatformFile

data class FileTreeNode(
    val file: KxFile,
    val name: String = file.resolveName(),
    val isDirectory: Boolean = file.isDirectory
)

fun KxFile.toFileTreeNode() = FileTreeNode(this)
fun PlatformFile.toFileTreeNode() = FileTreeNode(this.toKxFile())
fun List<KxFile>.toFileTreeNodes() = map { it.toFileTreeNode() }
