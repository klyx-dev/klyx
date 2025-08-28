package com.klyx.filetree

import com.klyx.core.file.KxFile
import com.klyx.core.file.resolveName
import com.klyx.core.file.toKxFile
import com.klyx.extension.api.Project
import com.klyx.extension.api.Worktree
import io.github.vinceglb.filekit.PlatformFile

data class FileTreeNode(
    val file: KxFile,
    val name: String = file.resolveName(),
    val isDirectory: Boolean = file.isDirectory
) {
    override fun hashCode(): Int {
        var result = isDirectory.hashCode()
        result = 31 * result + file.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FileTreeNode

        if (isDirectory != other.isDirectory) return false
        if (file.absolutePath != other.file.absolutePath) return false
        if (name != other.name) return false

        return true
    }
}

fun KxFile.toFileTreeNode() = FileTreeNode(this)
fun PlatformFile.toFileTreeNode() = FileTreeNode(this.toKxFile())
fun List<KxFile>.toFileTreeNodes() = map { it.toFileTreeNode() }

fun Project.toFileTreeNodes() = worktrees.map { it.asFileTreeNode() }

fun Worktree.asFileTreeNode() = FileTreeNode(rootFile)
