package com.klyx.editor.language

val QUERY_FILENAME_PREFIXES: Array<Pair<String, (LanguageQueries) -> String?>> = arrayOf(
    "highlights" to { it.highlights },
    "brackets" to { it.brackets },
    "indents" to { it.indents },
    "outline" to { it.outline },
    "embedding" to { it.embedding },
    "injections" to { it.injections },
    "overrides" to { it.overrides },
    "redactions" to { it.redactions },
    "runnables" to { it.runnables },
    "textobjects" to { it.textObjects },
    "debugger" to { it.debugger },
    "imports" to { it.imports },
)

/**
 * Tree-sitter language queries for a given language.
 */
data class LanguageQueries(
    var highlights: String? = null,
    var brackets: String? = null,
    var indents: String? = null,
    var outline: String? = null,
    var embedding: String? = null,
    var injections: String? = null,
    var overrides: String? = null,
    var redactions: String? = null,
    var runnables: String? = null,
    var textObjects: String? = null,
    var debugger: String? = null,
    var imports: String? = null,
)
