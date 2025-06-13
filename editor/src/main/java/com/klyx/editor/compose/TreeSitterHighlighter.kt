package com.klyx.editor.compose

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.klyx.editor.language.treesitter.getQueryScm
import com.klyx.treesitter.java.TreeSitterJava
import com.klyx.treesitter.json.TreeSitterJson
import com.klyx.treesitter.kotlin.TreeSitterKotlin
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Parser
import io.github.treesitter.ktreesitter.Query
import io.github.treesitter.ktreesitter.Tree

class TreeSitterHighlighter(context: Context) {
    private var parser: Parser? = null
    private var oldTree: Tree? = null
    private var language: Language? = null
    private var query: Query? = null

    private val kotlinQueries = context.getQueryScm("kotlin", "highlights")
    private val javaQueries = context.getQueryScm("java", "highlights")
    private val jsonQueries = context.getQueryScm("json", "highlights")

    fun setLanguage(languageName: String) {
        language = when (languageName.lowercase()) {
            "kotlin" -> Language(TreeSitterKotlin.language())
            "java" -> Language(TreeSitterJava.language())
            "json" -> Language(TreeSitterJson.language())
            else -> null
        }

        parser = language?.let { Parser(it) }

        query = when (languageName.lowercase()) {
            "kotlin" -> language?.let { Query(it, kotlinQueries) }
            "java" -> language?.let { Query(it, javaQueries) }
            "json" -> language?.let { Query(it, jsonQueries) }
            else -> null
        }
    }

    fun reparse(text: String) {
        if (parser == null) return
        oldTree = parser!!.parse(text, oldTree)
    }

    fun parse(text: String, oldTree: Tree? = null): Tree? {
        return parser?.parse(text, oldTree)
    }

    private fun byteOffsetToCharOffset(text: String, byteOffset: Int): Int {
        var charOffset = 0
        var byteCount = 0
        var i = 0

        while (i < text.length && byteCount < byteOffset) {
            val c = text[i]

            if (c == '\r') {
                if (i + 1 < text.length && text[i + 1] == '\n') {
                    // CRLF
                    byteCount += 2
                    i += 2
                    charOffset++
                    continue
                } else {
                    // CR
                    byteCount += 1
                    i++
                    charOffset++
                    continue
                }
            } else if (c == '\n') {
                // LF
                byteCount += 1
                i++
                charOffset++
                continue
            }

            // regular characters
            byteCount += when {
                c.code < 0x80 -> 1
                c.code < 0x800 -> 2
                c.code < 0x10000 -> 3
                else -> 4
            }
            i++
            charOffset++
        }

        return charOffset
    }

    fun getSyntaxHighlights(text: String): List<SyntaxHighlight> {
        val highlights = mutableListOf<SyntaxHighlight>()
        val tree = parse(text, oldTree) ?: return highlights
        oldTree = tree
        val query = this.query ?: return highlights

        query.matches(oldTree!!.rootNode).forEach { match ->
            match.captures.forEach { capture ->
                val node = capture.node
                val captureName = capture.name

                println(captureName)

                val color = when (captureName) {
                    "keyword" -> Color(0xFF569CD6).toArgb()
                    "keyword.function" -> Color(0xFF569CD6).toArgb()
                    "keyword.return" -> Color(0xFF569CD6).toArgb()
                    "string" -> Color(0xFFCE9178).toArgb()
                    "string.regex" -> Color(0xFFCE9178).toArgb()
                    "string.escape" -> Color(0xFFCE9178).toArgb()
                    "number" -> Color(0xFFB5CEA8).toArgb()
                    "float" -> Color(0xFFB5CEA8).toArgb()
                    "comment" -> Color(0xFF6A9955).toArgb()
                    "function" -> Color(0xFFDCDCAA).toArgb()
                    "function.builtin" -> Color(0xFFDCDCAA).toArgb()
                    "type" -> Color(0xFF4EC9B0).toArgb()
                    "type.builtin" -> Color(0xFF4EC9B0).toArgb()
                    "variable" -> Color(0xFF9CDCFE).toArgb()
                    "variable.builtin" -> Color(0xFF9CDCFE).toArgb()
                    "operator" -> Color(0xFFD4D4D4).toArgb()
                    "property" -> Color(0xFF9CDCFE).toArgb()
                    "property_key" -> Color(0xFF9CDCFE).toArgb()
                    "boolean", "true", "false" -> Color(0xFF569CD6).toArgb()
                    "null" -> Color(0xFF569CD6).toArgb()
                    "character" -> Color(0xFFCE9178).toArgb()
                    "parameter" -> Color(0xFF9CDCFE).toArgb()
                    "constructor" -> Color(0xFFDCDCAA).toArgb()
                    "constant" -> Color(0xFF4EC9B0).toArgb()
                    "label" -> Color(0xFFC586C0).toArgb()
                    "namespace" -> Color(0xFF4EC9B0).toArgb()
                    "include" -> Color(0xFF569CD6).toArgb()
                    "conditional" -> Color(0xFF569CD6).toArgb()
                    "repeat" -> Color(0xFF569CD6).toArgb()
                    "exception" -> Color(0xFF569CD6).toArgb()
                    "attribute" -> Color(0xFF4EC9B0).toArgb()
                    "punctuation.bracket" -> Color(0xFF808080).toArgb()
                    "punctuation.delimiter" -> Color(0xFF808080).toArgb()
                    "punctuation.special" -> Color(0xFF808080).toArgb()
                    else -> null
                }

                if (color != null) {
                    val startByte = node.startByte.toInt()
                    val endByte = node.endByte.toInt()

                    val startChar = byteOffsetToCharOffset(text, startByte)
                    val endChar = byteOffsetToCharOffset(text, endByte)

                    // only add highlight if it's within text bounds and valid
                    if (startChar < text.length && endChar <= text.length && startChar < endChar) {
                        highlights.add(
                            SyntaxHighlight(
                                startOffset = startChar,
                                endOffset = endChar,
                                color = color
                            )
                        )
                    }
                }
            }
        }

        return highlights.sortedBy { it.startOffset }
    }

    fun close() {
        oldTree?.close()
        parser?.close()
        query?.close()
    }
}

data class SyntaxHighlight(
    val startOffset: Int,
    val endOffset: Int,
    val color: Int
) 
