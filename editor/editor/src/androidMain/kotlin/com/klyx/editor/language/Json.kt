package com.klyx.editor.language

import com.itsaky.androidide.treesitter.json.TSLanguageJson
import io.github.rosemoe.sora.editor.ts.TsLanguage
import io.github.rosemoe.sora.editor.ts.TsLanguageSpec
import io.github.rosemoe.sora.lang.styling.textStyle
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.ATTRIBUTE_NAME
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMMENT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.IDENTIFIER_NAME
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LITERAL
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.OPERATOR

class JsonLanguage : TsLanguage(JsonLanguageSpec(), tab = false, themeDescription = {
    textStyle(LITERAL, bold = true) applyTo arrayOf("constant.builtin")
    textStyle(LITERAL) applyTo "string"
    textStyle(LITERAL) applyTo "number"
    textStyle(ATTRIBUTE_NAME) applyTo "property.name"
    textStyle(OPERATOR) applyTo arrayOf(
        "punctuation.bracket",
        "punctuation.delimiter"
    )
    textStyle(COMMENT, italic = true) applyTo "comment"
    textStyle(IDENTIFIER_NAME) applyTo "identifier"
    textStyle(HIGHLIGHTED_DELIMITERS_FOREGROUND) applyTo "editor.brackets"
})

private class JsonLanguageSpec : TsLanguageSpec(
    language = TSLanguageJson.getInstance(),
    highlightScmSource = """
        (pair
          key: (_) @string.special.key)

        (string) @string

        (number) @number

        [
          (null)
          (true)
          (false)
        ] @constant.builtin

        (escape_sequence) @escape

        (comment) @comment
    """.trimIndent(),
    codeBlocksScmSource = """
        (object) @scope.marked

        (array) @scope.marked
    """.trimIndent(),
    localsScmSource = """
        [
          (object)
          (array)
        ] @local.scope
    """.trimIndent(),
    bracketsScmSource = """
        (object
          "{" @editor.brackets.open
          "}" @editor.brackets.close)

        (array
          "[" @editor.brackets.open
          "]" @editor.brackets.close)
    """.trimIndent()
)
