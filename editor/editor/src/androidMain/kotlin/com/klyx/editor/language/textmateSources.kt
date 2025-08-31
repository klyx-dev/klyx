package com.klyx.editor.language

import com.klyx.editor.CodeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage

// https://github.com/Xed-Editor/Xed-Editor/blob/c22f0046639553371b57434d0feb0129d5e18973/core/main/src/main/java/com/rk/libcommons/editor/ScopeRegistry.kt#L3

val textmateSources = mapOf(
    "pro" to "source.shell",
    "java" to "source.java",
    "bsh" to "source.java",
    "html" to "text.html.basic",
    "htmx" to "text.html.htmx",
    "gsh" to "source.groovy",
    "kt" to "source.kotlin",
    "kts" to "source.kotlin",
    "py" to "source.python",
    "groovy" to "source.groovy",
    "nim" to "source.nim",
    "xml" to "text.xml",
    "go" to "source.go",
    "js" to "source.js",
    "ts" to "source.ts",
    "gradle" to "source.groovy",
    "tsx" to "source.tsx",
    "jsx" to "source.js.jsx",
    "md" to "text.html.markdown",
    "c" to "source.c",
    "bat" to "source.batchfile",
    "cpp" to "source.cpp",
    "h" to "source.cpp",
    "hpp" to "source.cpp",
    "xhtml" to "text.html.basic",
    "json" to "source.json",
    "css" to "source.css",
    "gvy" to "source.groovy",
    "cs" to "source.cs",
    "csx" to "source.cs",
    "xht" to "text.html.basic",
    "yml" to "source.yaml",
    "yaml" to "source.yaml",
    "gy" to "source.groovy",
    "cff" to "source.yaml",
    "cmd" to "source.batchfile",
    "sh" to "source.shell",
    "bash" to "source.shell",
    "htm" to "text.html.basic",
    "rs" to "source.rust",
    "lua" to "source.lua",
    "php" to "source.php",
    "ini" to "source.ini",
    "smali" to "source.smali",
    "v" to "source.coq",
    "coq" to "source.coq",
    "properties" to "source.java-properties",
    "mut" to "source.js",
    "latex" to "text.tex.latex",
    "tex" to "text.tex.latex",
    "ltx" to "text.tex.latex",
    "toml" to "source.toml",
    "dart" to "source.dart",
    "lisp" to "source.lisp",
    "sql" to "source.sql"
)

@OptIn(ExperimentalCodeEditorApi::class)
val CodeEditorState.languageScopeName get() = textmateSources[file.extension]

@OptIn(ExperimentalCodeEditorApi::class)
val CodeEditorState.textMateLanguageOrEmptyLanguage: Language
    get() = languageScopeName?.let { scopeName ->
        TextMateLanguage.create(
            scopeName,
            true
        )
    } ?: EmptyLanguage()
