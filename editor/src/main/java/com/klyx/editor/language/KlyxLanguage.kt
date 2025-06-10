package com.klyx.editor.language

import android.content.Context
import androidx.compose.runtime.Stable
import com.klyx.editor.language.treesitter.TSLanguage
import com.klyx.editor.language.treesitter.TSQuery
import com.klyx.editor.language.treesitter.getQueryScm
import com.klyx.editor.theme.HighlightToken
import com.klyx.editor.theme.KlyxColorScheme
import com.klyx.editor.theme.toCapture
import com.klyx.treesitter.java.TreeSitterJava
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Parser

class JavaLanguage(val context: Context) : KlyxLanguage {
    override val language: TSLanguage get() = Language(TreeSitterJava.language())

    override fun getHighlightsQuery(): TSQuery {
        val query = context.getQueryScm("java", "highlights")
        return TSQuery(language, query)
    }
}

@Stable
interface KlyxLanguage {
    val language: TSLanguage?

    fun getHighlightsQuery(): TSQuery?
}

internal fun KlyxLanguage.parse(parser: Parser, colorScheme: KlyxColorScheme, text: String): List<HighlightToken> {
    if (language == null || getHighlightsQuery() == null) return emptyList()

    val highlights = mutableListOf<HighlightToken>()
    val tree = parser.parse(text)
    val query = getHighlightsQuery() ?: return emptyList()

    query.matches(tree.rootNode).forEach { match ->
        match.captures.forEach { capture ->
            val node = capture.node
            val captureName = capture.name

            val color = colorScheme.getColor(captureName.toCapture() ?: Capture.TextLiteral)
            val start = node.startByte.toInt()
            val end = node.endByte.toInt()

            val startChar = text.byteOffsetToCharIndex(start)
            val endChar = text.byteOffsetToCharIndex(end)

            if (startChar < text.length && endChar <= text.length && startChar < endChar) {
                highlights.add(HighlightToken(startChar, endChar, color))
            }
        }
    }

    return highlights
}

fun String.byteOffsetToCharIndex(byteOffset: Int): Int {
    var charOffset = 0
    var byteCount = 0
    var i = 0

    while (i < this.length && byteCount < byteOffset) {
        val c = this[i]

        if (c == '\r') {
            if (i + 1 < this.length && this[i + 1] == '\n') {
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

