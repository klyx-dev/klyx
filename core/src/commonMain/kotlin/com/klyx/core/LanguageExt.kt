package com.klyx.core

import com.klyx.core.file.KxFile

fun KxFile.language() = when (extension.lowercase()) {
    "kt", "kts" -> "Kotlin"
    "json" -> "Json"
    "js" -> "JavaScript"
    "rs" -> "Rust"
    "java" -> "Java"
    "txt" -> "Plain Text"
    else -> "Plain Text"
}
