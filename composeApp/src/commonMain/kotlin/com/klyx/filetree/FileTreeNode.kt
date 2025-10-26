package com.klyx.filetree

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.ui.graphics.vector.ImageVector
import com.klyx.core.file.KxFile
import com.klyx.core.file.resolveName
import com.klyx.core.file.toKxFile
import com.klyx.core.icon.FolderOpen
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

fun Project.toFileTreeNodes() = worktrees.associateWith { it.asFileTreeNode() }

fun Worktree.asFileTreeNode() = FileTreeNode(rootFile)

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
        "json" -> Icons.Default.DataObject
        "apk" -> Icons.Default.Android
        "txt" -> Icons.Default.Description
        "png", "jpg", "jpeg", "gif", "svg", "webp" -> Icons.Default.Image
        "pdf" -> Icons.Default.PictureAsPdf
        "zip", "rar", "7z", "tar", "gz" -> Icons.Default.Archive
        "mp4", "avi", "mov", "mkv" -> Icons.Default.VideoFile
        "mp3", "wav", "flac", "ogg" -> Icons.Default.AudioFile
        "java", "h", "hpp", "cs" -> Icons.Default.Code
        else -> if (runCatching { file.canExecute }.getOrElse { false }) {
            Icons.Outlined.Terminal
        } else {
            Icons.AutoMirrored.Filled.InsertDriveFile
        }
    }
}

internal fun FileTreeNode.resolveFolderIcon(isExpanded: Boolean): ImageVector {
    return if (isExpanded) {
        KlyxIcons.FolderOpen
    } else {
        Icons.Outlined.Folder
    }
}
