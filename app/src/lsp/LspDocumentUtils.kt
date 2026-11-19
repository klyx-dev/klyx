package com.klyx.lsp

import com.klyx.core.utils.actualUri
import com.klyx.runtime.workspace.Document
import com.klyx.runtime.workspace.DocumentLocation
import com.klyx.runtime.workspace.Workspace
import io.github.lumkit.sweeteditor.highlight.HighlightDocumentDescriptor
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.resolve

object LspDocumentUtils {

    fun uriOfOrNull(document: Document, workspace: Workspace): String? =
        fileOf(document, workspace)?.let { (termuxSnapshotSource(it) ?: it).actualUri() }

    fun uriOf(document: Document, workspace: Workspace): String =
        requireNotNull(uriOfOrNull(document, workspace)) { "Document has no workspace file" }

    fun fileOf(document: Document, workspace: Workspace): PlatformFile? = when (val location = document.location) {
        is DocumentLocation.Platform -> location.file
        is DocumentLocation.Project -> {
            val root = workspace.projects.value[location.projectId]?.root
            root?.resolve(location.relativePath.toString())
        }
    }

    private val standardHeaders = """
        algorithm any array atomic barrier bit bitset cassert cctype cerrno cfenv cfloat charconv chrono
        cinttypes ciso646 climits clocale cmath codecvt compare complex concepts condition_variable coroutine
        csetjmp csignal cstdarg cstddef cstdint cstdio cstdlib cstring ctime cuchar cwchar cwctype deque
        exception execution expected filesystem flat_map flat_set format forward_list fstream functional future
        generator initializer_list iomanip ios iosfwd iostream istream iterator latch limits list locale map
        mdspan memory memory_resource mutex new numbers numeric optional ostream print queue random ranges
        ratio regex scoped_allocator semaphore set shared_mutex source_location span spanstream sstream stack
        stacktrace stdexcept stop_token streambuf string string_view strstream syncstream system_error thread
        tuple type_traits typeindex typeinfo unordered_map unordered_set utility valarray variant vector version
    """.trimIndent().split(Regex("\\s+")).toSet()

    private fun isCppHeader(document: Document): Boolean {
        if ('.' in document.name) return false
        if (document.name in standardHeaders) return true
        val path = when (val location = document.location) {
            is DocumentLocation.Platform -> location.file.path
            is DocumentLocation.Project -> location.relativePath.toString()
        }
        return "/include/c++/" in "/" + path.replace('\\', '/')
    }

    fun highlightDescriptor(document: Document): HighlightDocumentDescriptor = when {
        document.name.endsWith(".pyi", ignoreCase = true) -> {
            HighlightDocumentDescriptor(
                fileName = document.name,
                syntaxName = "python"
            )
        }

        isCppHeader(document) -> HighlightDocumentDescriptor(fileName = "${document.name}.cpp", syntaxName = "cpp")
        else -> HighlightDocumentDescriptor(fileName = document.name)
    }

    fun languageIdOf(document: Document): String {
        if (isCppHeader(document)) return "cpp"
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
}
