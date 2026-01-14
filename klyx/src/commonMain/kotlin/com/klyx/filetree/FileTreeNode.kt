package com.klyx.filetree

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import arrow.core.raise.context.result
import com.klyx.core.file.KxFile
import com.klyx.core.file.resolveName
import com.klyx.core.icon.KlyxIcons
import com.klyx.core.icon.language.C
import com.klyx.core.icon.language.Cpp
import com.klyx.core.icon.language.Css
import com.klyx.core.icon.language.Go
import com.klyx.core.icon.language.Html
import com.klyx.core.icon.language.JavaScript
import com.klyx.core.icon.language.Kotlin
import com.klyx.core.icon.language.Markdown
import com.klyx.core.icon.language.Php
import com.klyx.core.icon.language.Python
import com.klyx.core.icon.language.React
import com.klyx.core.icon.language.Ruby
import com.klyx.core.icon.language.Rust
import com.klyx.core.icon.language.Sass
import com.klyx.core.icon.language.Swift
import com.klyx.core.icon.language.Toml
import com.klyx.core.icon.language.TypeScript
import com.klyx.core.icon.language.Xml
import com.klyx.core.icon.language.Yaml
import com.klyx.icons.Android
import com.klyx.icons.Archive
import com.klyx.icons.AudioFile
import com.klyx.icons.Code
import com.klyx.icons.DataObject
import com.klyx.icons.Description
import com.klyx.icons.Folder
import com.klyx.icons.FolderOpen
import com.klyx.icons.Icons
import com.klyx.icons.Image
import com.klyx.icons.InsertDriveFile
import com.klyx.icons.PictureAsPdf
import com.klyx.icons.Terminal
import com.klyx.icons.VideoFile
import com.klyx.project.Project
import com.klyx.project.Worktree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

@Immutable
data class FileTreeNode(
    val file: KxFile,
    val name: String = file.resolveName(),
    val isDirectory: Boolean = file.isDirectory,
    val isExecutable: Boolean = result { file.canExecute }.getOrElse { false },
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

suspend fun KxFile.toFileTreeNode() = withContext(Dispatchers.IO) { FileTreeNode(this@toFileTreeNode) }
suspend fun List<KxFile>.toFileTreeNodes() = map { it.toFileTreeNode() }

suspend fun Project.toFileTreeNodes() = worktrees.associateWith { it.asFileTreeNode() }
suspend fun Worktree.asFileTreeNode() = withContext(Dispatchers.IO) { FileTreeNode(rootFile) }

internal fun FileTreeNode.resolveFileIcon(): ImageVector {
    return when (file.extension) {
        "kt", "kts" -> KlyxIcons.Language.Kotlin
        "js" -> KlyxIcons.Language.JavaScript
        "ts" -> KlyxIcons.Language.TypeScript
        "tsx", "jsx" -> KlyxIcons.Language.React
        "c" -> KlyxIcons.Language.C
        "cpp" -> KlyxIcons.Language.Cpp
        "go" -> KlyxIcons.Language.Go
        "rs" -> KlyxIcons.Language.Rust
        "swift" -> KlyxIcons.Language.Swift
        "php" -> KlyxIcons.Language.Php
        "rb" -> KlyxIcons.Language.Ruby
        "html", "htm" -> KlyxIcons.Language.Html
        "css" -> KlyxIcons.Language.Css
        "scss", "sass" -> KlyxIcons.Language.Sass
        "xml" -> KlyxIcons.Language.Xml
        "toml" -> KlyxIcons.Language.Toml
        "py" -> KlyxIcons.Language.Python
        "yml", "yaml" -> KlyxIcons.Language.Yaml
        "md", "mdx" -> KlyxIcons.Language.Markdown
        "json" -> Icons.DataObject
        "apk" -> Icons.Android
        "txt" -> Icons.Description
        "png", "jpg", "jpeg", "gif", "svg", "webp" -> Icons.Image
        "pdf" -> Icons.PictureAsPdf
        "zip", "rar", "7z", "tar", "gz" -> Icons.Archive
        "mp4", "avi", "mov", "mkv" -> Icons.VideoFile
        "mp3", "wav", "flac", "ogg" -> Icons.AudioFile
        "java", "h", "hpp", "cs" -> Icons.Code
        else -> if (isExecutable) {
            Icons.Terminal
        } else {
            Icons.InsertDriveFile
        }
    }
}

internal fun FileTreeNode.resolveFolderIcon(isExpanded: Boolean): ImageVector {
    return if (isExpanded) {
        Icons.FolderOpen
    } else {
        Icons.Folder
    }
}
