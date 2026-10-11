package com.klyx.lsp

import com.klyx.runtime.workspace.Document
import com.klyx.runtime.workspace.DocumentLocation
import com.klyx.runtime.workspace.Workspace
import okio.Path
import okio.Path.Companion.toPath

object LspDocumentUtils {

    fun uriOf(document: Document, workspace: Workspace): String = when (val loc = document.location) {
        is DocumentLocation.Local -> pathToUri(loc.path)
        is DocumentLocation.Project -> {
            val root = workspace.projects.value[loc.projectId]?.root
            if (root != null) {
                pathToUri(root / loc.relativePath)
            } else {
                "file:///${loc.relativePath}"
            }
        }

        is DocumentLocation.Uri -> loc.value
    }

    fun pathOf(document: Document, workspace: Workspace): Path? = when (val loc = document.location) {
        is DocumentLocation.Local -> loc.path
        is DocumentLocation.Project -> {
            val root = workspace.projects.value[loc.projectId]?.root
            if (root != null) root / loc.relativePath else null
        }

        is DocumentLocation.Uri -> {
            if (loc.value.startsWith("file://")) {
                loc.value.removePrefix("file://").toPath()
            } else {
                null
            }
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

    private fun pathToUri(path: Path): String {
        val s = path.toString()
        val normalized = if (s.startsWith("/")) s else "/$s"
        return "file://$normalized"
    }
}
