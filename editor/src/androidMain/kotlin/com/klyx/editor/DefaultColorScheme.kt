package com.klyx.editor

import androidx.core.graphics.toColorInt
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

object DefaultColorScheme : EditorColorScheme(true) {
    override fun applyDefault() {
        super.applyDefault()

        setColor(WHOLE_BACKGROUND, "#282c34".toColorInt())
        setColor(TEXT_NORMAL, "#abb2bf".toColorInt())

        setColor(COMMENT, "#5c6370".toColorInt())
        setColor(KEYWORD, "#c678dd".toColorInt())
        setColor(FUNCTION_NAME, "#61afef".toColorInt())
        setColor(LITERAL, "#98c379".toColorInt())
        setColor(OPERATOR, "#56b6c2".toColorInt())

        setColor(IDENTIFIER_NAME, "#abb2bf".toColorInt())
        setColor(IDENTIFIER_VAR, "#e06c75".toColorInt())
        setColor(ATTRIBUTE_NAME, "#d19a66".toColorInt())

        setColor(LINE_NUMBER_BACKGROUND, "#282c34".toColorInt())
        setColor(LINE_NUMBER, "#4b5263".toColorInt())
        setColor(LINE_NUMBER_CURRENT, "#abb2bf".toColorInt())

        setColor(CURRENT_LINE, "#2c313c".toColorInt())
        setColor(SELECTED_TEXT_BACKGROUND, "#3e4451".toColorInt())
        setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, "#abb2bf".toColorInt())
        setColor(NON_PRINTABLE_CHAR, "#5c6370".toColorInt())
        setColor(HARD_WRAP_MARKER, "#3e445144".toColorInt())
    }
}
