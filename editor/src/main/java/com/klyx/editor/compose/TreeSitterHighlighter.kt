package com.klyx.editor.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.itsaky.androidide.treesitter.java.TSLanguageJava
import com.itsaky.androidide.treesitter.json.TSLanguageJson
import com.itsaky.androidide.treesitter.kotlin.TSLanguageKotlin
import com.itsaky.androidide.treesitter.python.TSLanguagePython
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Parser
import io.github.treesitter.ktreesitter.Tree

class TreeSitterHighlighter {
    private var parser: Parser? = null
    private var tree: Tree? = null
    private var language: Language? = null

    fun setLanguage(languageName: String) {
        language = when (languageName.lowercase()) {
            "kotlin" -> Language(TSLanguageKotlin.getInstance().nativeObject)
            "java" -> Language(TSLanguageJava.getInstance().nativeObject)
            "python" -> Language(TSLanguagePython.getInstance().nativeObject)
            "json" -> Language(TSLanguageJson.getInstance().nativeObject)
            else -> null
        }

        parser = language?.let { Parser(it) }
    }

    fun parse(text: String): Tree? {
        return parser?.parse(text)
    }

    private fun byteOffsetToCharOffset(text: String, byteOffset: Int): Int {
        var charOffset = 0
        var byteCount = 0
        while (charOffset < text.length && byteCount < byteOffset) {
            val c = text[charOffset]
            byteCount += when {
                c.code < 0x80 -> 1
                c.code < 0x800 -> 2
                c.code < 0x10000 -> 3
                else -> 4
            }
            charOffset++
        }
        return charOffset
    }

    fun getSyntaxHighlights(text: String): List<SyntaxHighlight> {
        val highlights = mutableListOf<SyntaxHighlight>()
        val tree = parse(text) ?: return highlights

        fun processNode(node: Node) {
            val type = node.type

            println(type)

            val color = when (type) {
                "keyword", "keyword_control" -> Color(0xFF569CD6).toArgb()
                "string", "string_content" -> Color(0xFFCE9178).toArgb()
                "number" -> Color(0xFFB5CEA8).toArgb()
                "comment" -> Color(0xFF6A9955).toArgb()
                "function", "method" -> Color(0xFFDCDCAA).toArgb()
                "type", "class" -> Color(0xFF4EC9B0).toArgb()
                "variable", "parameter" -> Color(0xFF9CDCFE).toArgb()
                "operator" -> Color(0xFFD4D4D4).toArgb()

                "property_key", "property_identifier" -> Color(0xFF9CDCFE).toArgb()
                "string_value" -> Color(0xFFCE9178).toArgb()
                "number_value" -> Color(0xFFB5CEA8).toArgb()
                "boolean_value", "true", "false" -> Color(0xFF569CD6).toArgb()
                "null_value", "null" -> Color(0xFF569CD6).toArgb()

                "{", "}" -> Color(0xFF569CD6).toArgb()
                "(", ")" -> Color(0xFF569CD6).toArgb()
                "[", "]" -> Color(0xFF569CD6).toArgb()

                else -> null
            }

            if (color != null) {
                val startByte = node.startByte.toInt()
                val endByte = node.endByte.toInt()

                // Convert byte offsets to character offsets
                val startChar = byteOffsetToCharOffset(text, startByte)
                val endChar = byteOffsetToCharOffset(text, endByte)

                // Only add highlight if it's within text bounds and valid
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

            for (i in 0 until node.childCount.toInt()) {
                node.child(i.toUInt())?.let { processNode(it) }
            }
        }

        processNode(tree.rootNode)
        return highlights.sortedBy { it.startOffset }
    }

    fun close() {
        tree?.close()
        parser?.close()
    }
}

data class SyntaxHighlight(
    val startOffset: Int,
    val endOffset: Int,
    val color: Int
) 
