package com.klyx.editor.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.klyx.editor.language.Capture

fun String.toCapture(): Capture? {
    val enumName = split(".").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    return try {
        Capture.valueOf(enumName)
    } catch (e: IllegalArgumentException) {
        null
    }
}

@Immutable
interface KlyxColorScheme {
    fun getColor(capture: Capture): Color
}

@Immutable
object DefaultColorScheme : KlyxColorScheme {
    override fun getColor(capture: Capture): Color = when (capture) {
        Capture.Keyword,
        Capture.Preproc,
        Capture.TagDoctype,
        Capture.Predictive -> Color(0xFFCC7832)

        Capture.String,
        Capture.StringSpecial,
        Capture.StringEscape,
        Capture.StringSpecialSymbol,
        Capture.StringRegex -> Color(0xFF6A8759)

        Capture.Number,
        Capture.Constant,
        Capture.Boolean -> Color(0xFF6897BB)

        Capture.Function,
        Capture.Constructor -> Color(0xFF9876AA)

        Capture.Type,
        Capture.Enum,
        Capture.Tag,
        Capture.Variant -> Color(0xFFBBB529)

        Capture.Variable,
        Capture.VariableSpecial -> Color(0xFFA9B7C6)

        Capture.Attribute,
        Capture.Property,
        Capture.Label -> Color(0xFF9876AA)

        Capture.Comment,
        Capture.CommentDoc -> Color(0xFF808080)

        Capture.Operator,
        Capture.Punctuation,
        Capture.PunctuationBracket,
        Capture.PunctuationDelimiter,
        Capture.PunctuationListMarker,
        Capture.PunctuationSpecial -> Color(0xFFB4B4B4)

        Capture.LinkText,
        Capture.Hint,
        Capture.LintUri -> Color(0xFF287BDE)

        Capture.Emphasis,
        Capture.EmphasisStrong,
        Capture.TextLiteral,
        Capture.Title -> Color(0xFFD0A040)

        Capture.Embedded -> Color(0xFFC55450)

        Capture.Primary -> Color(0xFFCCCCCC)
    }
}

