package com.klyx.editor.language

import com.klyx.core.file.FileWrapper
import com.klyx.core.file.extension

fun FileWrapper.language() = when (val ext = extension.lowercase()) {
    "kt", "kts" -> "kotlin"
    "js" -> "javascript"
    "ts" -> "typescript"
    "py" -> "python"
    "rs" -> "rust"
    "cpp", "cc", "cxx" -> "cpp"
    "cs" -> "csharp"
    "htm" -> "html"
    "sh" -> "bash"
    else -> ext.takeIf {
        it in setOf(
            "java", "json", "xml", "html", "css", "swift", "go", "c", "bash"
        )
    } ?: "text"
}
