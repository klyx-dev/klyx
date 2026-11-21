package com.klyx.lsp

import io.github.lumkit.sweeteditor.FoldRegion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class SmartSnippet(
    val label: String,
    val detail: String,
    val body: String,
)

object SmartLanguageFeatures {

    fun keywordsFor(languageId: String): List<String> = when (languageId.lowercase()) {
        "kotlin" -> listOf(
            "as", "as?", "break", "class", "continue", "do", "else", "false", "for", "fun",
            "if", "in", "!in", "interface", "is", "!is", "null", "object", "package", "return",
            "super", "this", "throw", "true", "try", "typealias", "val", "var", "when", "while",
            "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally",
            "get", "import", "init", "param", "property", "receiver", "set", "setparam",
            "where", "actual", "abstract", "annotation", "companion", "const", "crossinline",
            "data", "enum", "expect", "external", "final", "infix", "inline", "inner",
            "internal", "lateinit", "noinline", "open", "operator", "out", "override",
            "private", "protected", "public", "reified", "sealed", "suspend", "tailrec", "vararg"
        )

        "python" -> listOf(
            "and", "as", "assert", "async", "await", "break", "class", "continue", "def",
            "del", "elif", "else", "except", "False", "finally", "for", "from", "global",
            "if", "import", "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass",
            "raise", "return", "True", "try", "while", "with", "yield", "self", "cls"
        )

        "rust" -> listOf(
            "as", "async", "await", "break", "const", "continue", "crate", "dyn", "else",
            "enum", "extern", "false", "fn", "for", "if", "impl", "in", "let", "loop",
            "match", "mod", "move", "mut", "pub", "ref", "return", "self", "Self", "static",
            "struct", "super", "trait", "true", "type", "unsafe", "use", "where", "while"
        )

        "c", "cpp" -> listOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
            "else", "enum", "extern", "float", "for", "goto", "if", "int", "long", "register",
            "return", "short", "signed", "sizeof", "static", "struct", "switch", "typedef",
            "union", "unsigned", "void", "volatile", "while", "class", "namespace", "new",
            "delete", "public", "private", "protected", "template", "typename", "this",
            "virtual", "override", "constexpr", "nullptr", "bool", "true", "false", "include"
        )

        "go" -> listOf(
            "break", "case", "chan", "const", "continue", "default", "defer", "else",
            "fallthrough", "for", "func", "go", "goto", "if", "import", "interface",
            "map", "package", "range", "return", "select", "struct", "switch", "type",
            "var", "true", "false", "iota", "nil", "make", "new", "len", "cap", "append"
        )

        "typescript", "javascript" -> listOf(
            "break", "case", "catch", "class", "const", "continue", "debugger", "default",
            "delete", "do", "else", "export", "extends", "finally", "for", "function",
            "if", "import", "in", "instanceof", "new", "return", "super", "switch", "this",
            "throw", "try", "typeof", "var", "void", "while", "with", "yield", "let",
            "static", "enum", "await", "async", "null", "undefined", "true", "false",
            "interface", "type", "implements", "private", "protected", "public", "readonly"
        )

        "java" -> listOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while", "true", "false", "null", "record"
        )

        "json" -> listOf("true", "false", "null")
        "sql" -> listOf(
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
            "CREATE", "TABLE", "DROP", "ALTER", "ADD", "COLUMN", "PRIMARY", "KEY", "FOREIGN",
            "REFERENCES", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "ON", "GROUP", "BY",
            "HAVING", "ORDER", "ASC", "DESC", "LIMIT", "OFFSET", "UNION", "ALL", "DISTINCT",
            "AND", "OR", "NOT", "IN", "IS", "NULL", "LIKE", "BETWEEN", "CASE", "WHEN", "THEN", "ELSE", "END"
        )

        "shellscript" -> listOf(
            "if", "then", "else", "elif", "fi", "case", "esac", "for", "while", "until",
            "do", "done", "in", "function", "select", "time", "return", "exit", "echo",
            "export", "local", "readonly", "set", "unset", "shift", "test", "trap"
        )

        "html", "xml" -> listOf(
            "html", "head", "body", "div", "span", "p", "a", "img", "ul", "ol", "li",
            "table", "tr", "td", "th", "form", "input", "button", "script", "style", "link", "meta"
        )

        "css" -> listOf(
            "color", "background", "margin", "padding", "border", "display", "flex", "grid",
            "position", "width", "height", "font-size", "font-family", "font-weight", "line-height",
            "text-align", "justify-content", "align-items", "z-index", "overflow", "opacity"
        )

        else -> emptyList()
    }

    fun snippetsFor(languageId: String): List<SmartSnippet> = when (languageId.lowercase()) {
        "kotlin" -> listOf(
            SmartSnippet("fun", "Function definition", $$"fun ${1:name}(${2:params}): ${3:Unit} {\n    ${0}\n}"),
            SmartSnippet("val", "Read-only property", $$"val ${1:name}: ${2:Type} = ${0}"),
            SmartSnippet("var", "Mutable property", $$"var ${1:name}: ${2:Type} = ${0}"),
            SmartSnippet("class", "Class definition", $$"class ${1:Name}(${2:params}) {\n    ${0}\n}"),
            SmartSnippet("data class", "Data class", $$"data class ${1:Name}(\n    val ${2:field}: ${3:String},\n)"),
            SmartSnippet("if", "If expression", $$"if (${1:condition}) {\n    ${0}\n}"),
            SmartSnippet("ifelse", "If-else expression", $$"if (${1:condition}) {\n    ${2}\n} else {\n    ${0}\n}"),
            SmartSnippet(
                "when",
                "When expression",
                $$"when (${1:value}) {\n    ${2:pattern} -> ${0}\n    else -> {}\n}"
            ),
            SmartSnippet("for", "For loop", $$"for (${1:item} in ${2:collection}) {\n    ${0}\n}"),
        )

        "python" -> listOf(
            SmartSnippet("def", "Function definition", $$"def ${1:name}(${2:args}):\n    ${0:pass}"),
            SmartSnippet(
                "class",
                "Class definition",
                $$"class ${1:Name}:\n    def __init__(self${2:, args}):\n        ${0:pass}"
            ),
            SmartSnippet("if", "If statement", $$"if ${1:condition}:\n    ${0:pass}"),
            SmartSnippet("ifelse", "If-Else statement", $$"if ${1:condition}:\n    ${2:pass}\nelse:\n    ${0:pass}"),
            SmartSnippet("for", "For loop", $$"for ${1:item} in ${2:iterable}:\n    ${0:pass}"),
            SmartSnippet("with", "With context", $$"with ${1:expr} as ${2:target}:\n    ${0:pass}"),
        )

        "rust" -> listOf(
            SmartSnippet("fn", "Function definition", "fn \${1:name}(\${2:args}) -> \${3:()} {\n    \${0}\n}"),
            SmartSnippet("pub fn", "Public function", "pub fn \${1:name}(\${2:args}) -> \${3:()} {\n    \${0}\n}"),
            SmartSnippet("struct", "Struct definition", "struct \${1:Name} {\n    \${0}\n}"),
            SmartSnippet("impl", "Implementation block", "impl \${1:Type} {\n    \${0}\n}"),
            SmartSnippet("match", "Match expression", "match \${1:expr} {\n    \${2:pattern} => \${0},\n}"),
            SmartSnippet("let", "Let binding", "let \${1:name} = \${0};"),
            SmartSnippet("let mut", "Mutable let binding", "let mut \${1:name} = \${0};"),
        )

        "c", "cpp" -> listOf(
            SmartSnippet("fn", "Function definition", "\${1:void} \${2:name}(\${3:args}) {\n    \${0}\n}"),
            SmartSnippet("if", "If statement", "if (\${1:condition}) {\n    \${0}\n}"),
            SmartSnippet("for", "For loop", "for (int \${1:i} = 0; \${1:i} < \${2:n}; ++\${1:i}) {\n    \${0}\n}"),
            SmartSnippet("struct", "Struct definition", "struct \${1:Name} {\n    \${0}\n};"),
            SmartSnippet("inc", "#include directive", "#include <\${1:stdio.h}>"),
        )

        "go" -> listOf(
            SmartSnippet("func", "Function definition", "func \${1:name}(\${2:params}) \${3:error} {\n    \${0}\n}"),
            SmartSnippet("if", "If statement", "if \${1:condition} {\n    \${0}\n}"),
            SmartSnippet("for", "For loop", "for \${1:i} := 0; \${1:i} < \${2:n}; \${1:i}++ {\n    \${0}\n}"),
            SmartSnippet("struct", "Struct definition", "type \${1:Name} struct {\n    \${0}\n}"),
            SmartSnippet("err", "Error check", "if err != nil {\n    return \${0:err}\n}"),
        )

        "typescript", "javascript" -> listOf(
            SmartSnippet("fn", "Function", "function \${1:name}(\${2:args}) {\n    \${0}\n}"),
            SmartSnippet("afn", "Arrow function", "(\${1:args}) => {\n    \${0}\n}"),
            SmartSnippet("const", "Constant declaration", "const \${1:name} = \${0};"),
            SmartSnippet("let", "Variable declaration", "let \${1:name} = \${0};"),
            SmartSnippet("if", "If statement", "if (\${1:condition}) {\n    \${0}\n}"),
            SmartSnippet(
                "for",
                "For loop",
                "for (let \${1:i} = 0; \${1:i} < \${2:arr}.length; \${1:i}++) {\n    \${0}\n}"
            ),
            SmartSnippet("clg", "Console log", "console.log(\${1:message});"),
        )

        else -> emptyList()
    }

    private val wordRegex = Regex("""\b[a-zA-Z_][a-zA-Z0-9_]{1,60}\b""")

    fun extractWords(text: String, prefix: String, maxWords: Int = 100): List<String> {
        val counts = mutableMapOf<String, Int>()
        for (match in wordRegex.findAll(text)) {
            val w = match.value
            if (prefix.isEmpty() || w.contains(prefix, ignoreCase = true)) {
                counts[w] = (counts[w] ?: 0) + 1
            }
        }
        return counts.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> {
                    if (prefix.isNotEmpty() && it.key.startsWith(prefix, ignoreCase = true)) 1 else 0
                }.thenByDescending { it.value }
            )
            .take(maxWords)
            .map { it.key }
    }

    fun getWordAt(lineStr: String, col: Int): String {
        if (lineStr.isEmpty()) return ""
        val c = col.coerceIn(0, lineStr.length)
        var start = c
        while (start > 0 && (lineStr[start - 1].isLetterOrDigit() || lineStr[start - 1] == '_')) {
            start--
        }
        var end = c
        while (end < lineStr.length && (lineStr[end].isLetterOrDigit() || lineStr[end] == '_')) {
            end++
        }
        return if (start < end) lineStr.substring(start, end) else ""
    }

    fun formatTextFallback(rawText: String, extension: String): String {
        if (rawText.isEmpty()) return rawText
        if (extension == "json") {
            try {
                val prettyJson = Json { prettyPrint = true }
                val parsed = prettyJson.decodeFromString(JsonElement.serializer(), rawText)
                return prettyJson.encodeToString(JsonElement.serializer(), parsed) + "\n"
            } catch (_: Throwable) {
                // Ignore json parsing error and fall back to whitespace trim
            }
        }
        val lines = rawText.lines()
        val trimmedLines = lines.map { it.trimEnd() }
        val joined = trimmedLines.joinToString("\n").trimEnd()
        return if (joined.isNotEmpty()) "$joined\n" else ""
    }
}

object SyntacticFolding {

    fun computeFoldRegions(text: String): List<FoldRegion> {
        val lines = text.lines()
        if (lines.size < 3) return emptyList()
        val regions = mutableListOf<FoldRegion>()

        val braceStack = mutableListOf<Int>()
        val bracketStack = mutableListOf<Int>()
        var inBlockComment = false
        var blockCommentStart = -1

        for ([lineIdx, line] in lines.withIndex()) {
            val trimmed = line.trim()
            if (inBlockComment) {
                if (trimmed.contains("*/") || trimmed.contains("-->")) {
                    if (blockCommentStart in 0..<lineIdx) {
                        regions.add(FoldRegion(startLine = blockCommentStart, endLine = lineIdx, collapsed = false))
                    }
                    inBlockComment = false
                    blockCommentStart = -1
                }
                continue
            }
            if (trimmed.startsWith("/*") || trimmed.startsWith("<!--")) {
                if (!trimmed.contains("*/") && !trimmed.contains("-->")) {
                    inBlockComment = true
                    blockCommentStart = lineIdx
                }
            }

            for (char in line) {
                if (char == '{') braceStack.add(lineIdx)
                else if (char == '}' && braceStack.isNotEmpty()) {
                    val start = braceStack.removeAt(braceStack.size - 1)
                    if (lineIdx > start) {
                        regions.add(FoldRegion(startLine = start, endLine = lineIdx, collapsed = false))
                    }
                } else if (char == '[') bracketStack.add(lineIdx)
                else if (char == ']' && bracketStack.isNotEmpty()) {
                    val start = bracketStack.removeAt(bracketStack.size - 1)
                    if (lineIdx > start) {
                        regions.add(FoldRegion(startLine = start, endLine = lineIdx, collapsed = false))
                    }
                }
            }
        }

        // Indentation-based folding (for Python, YAML, and general indented structures)
        val indentLevels = lines.map { l ->
            if (l.isBlank()) -1 else l.takeWhile { it == ' ' || it == '\t' }.length
        }
        for (i in 0 until lines.size - 1) {
            val currentIndent = indentLevels[i]
            if (currentIndent < 0) continue
            var endLine = -1
            for (j in i + 1 until lines.size) {
                val nextIndent = indentLevels[j]
                if (nextIndent < 0) continue
                if (nextIndent > currentIndent) {
                    endLine = j
                } else {
                    break
                }
            }
            if (endLine > i + 1) {
                if (regions.none { it.startLine == i && it.endLine == endLine }) {
                    regions.add(FoldRegion(startLine = i, endLine = endLine, collapsed = false))
                }
            }
        }

        return regions
            .distinctBy { "${it.startLine}-${it.endLine}" }
            .sortedBy { it.startLine }
    }
}
