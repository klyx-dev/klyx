package com.klyx.lsp

import com.klyx.core.utils.actualUri
import com.klyx.core.utils.join
import com.klyx.runtime.workspace.Document
import com.klyx.runtime.workspace.DocumentLocation
import com.klyx.runtime.workspace.Workspace
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.resolve
import io.github.vinceglb.filekit.toKotlinxIoPath
import kotlinx.io.files.Path

object LspDocumentUtils {

    fun uriOf(document: Document, workspace: Workspace): String = when (val loc = document.location) {
        is DocumentLocation.Platform -> loc.file.actualUri()
        is DocumentLocation.Project -> {
            val root = workspace[loc.projectId].root
            root.resolve(loc.relativePath.toString()).actualUri()
        }
    }

    fun fileOf(document: Document, workspace: Workspace): PlatformFile? = when (val location = document.location) {
        is DocumentLocation.Platform -> location.file
        is DocumentLocation.Project -> {
            val root = workspace.projects.value[location.projectId]?.root
            root?.resolve(location.relativePath.toString())
        }
    }

    fun languageIdOf(document: Document): String {
        val name = document.name
        val ext = name.substringAfterLast('.', "").lowercase()
        val config = LspServerRegistry.findByExtension(ext)
        if (config != null) return config.languageId

        return when (ext) {
            "kt", "kts" -> "kotlin"
            "py", "pyi" -> "python"
            "rs" -> "rust"
            "c", "h" -> "c"
            "cpp", "hpp", "cc", "cxx" -> "cpp"
            "go" -> "go"
            "java" -> "java"
            "js", "jsx", "mjs", "cjs" -> "javascript"
            "ts", "tsx", "mts", "cts" -> "typescript"
            "json" -> "json"
            "html", "htm" -> "html"
            "css", "scss", "less" -> "css"
            "xml" -> "xml"
            "yaml", "yml" -> "yaml"
            "md", "markdown" -> "markdown"
            "sh", "bash" -> "shellscript"
            "sql" -> "sql"
            "toml" -> "toml"
            else -> "plaintext"
        }
    }

    private fun pathToUri(path: String): String {
        val normalized = if (path.startsWith("/")) path else "/$path"
        return "file://$normalized"
    }
}
