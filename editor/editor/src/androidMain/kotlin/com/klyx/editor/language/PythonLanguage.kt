package com.klyx.editor.language

import android.annotation.SuppressLint
import android.content.Context
import com.klyx.core.ContextHolder
import com.klyx.editor.treesitter.createSoraTsLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.styling.textStyle
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.ANNOTATION
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.ATTRIBUTE_NAME
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMMENT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.FUNCTION_NAME
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.IDENTIFIER_NAME
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.IDENTIFIER_VAR
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.KEYWORD
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LITERAL
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.OPERATOR

@SuppressLint("SdCardPath")
fun pythonLang(context: Context, wrapperLanguage: Language? = null) = createSoraTsLanguage(
    context,
    "/sdcard/Klyx/python/arm64-v8a/libtree-sitter-python.so",
    languageName = "python",
    highlightScmSource = ContextHolder.context.assets.open("highlights.scm").bufferedReader().readText(),
    codeBlocksScmSource = "(block) @scope.marked",
    localsScmSource = """
        (block) @local.scope
        (function_definition body: (block) @local.scope)
        (class_definition body: (block) @local.scope)
    """.trimIndent(),
    bracketsScmSource = """
        (list
          "[" @editor.brackets.open
          "]" @editor.brackets.close)

        (dictionary
          "{" @editor.brackets.open
          "}" @editor.brackets.close)

        (parenthesized_expression
          "(" @editor.brackets.open
          ")" @editor.brackets.close)
    """.trimIndent(),
    wrapperLanguage = wrapperLanguage
) {
    textStyle(KEYWORD, bold = true) applyTo "keyword"

    textStyle(FUNCTION_NAME) applyTo arrayOf("function", "function.method")
    textStyle(FUNCTION_NAME, italic = true) applyTo "function.builtin"

    textStyle(ANNOTATION) applyTo "constructor"
    textStyle(ANNOTATION, italic = true) applyTo "type"

    textStyle(IDENTIFIER_NAME) applyTo "variable"
    textStyle(IDENTIFIER_VAR, italic = true) applyTo "property"

    textStyle(LITERAL) applyTo arrayOf("string", "number", "constant.builtin")
    textStyle(OPERATOR) applyTo "operator"
    textStyle(COMMENT, italic = true) applyTo "comment"
    textStyle(ATTRIBUTE_NAME, bold = true, italic = true) applyTo "function.decorator"

    textStyle(HIGHLIGHTED_DELIMITERS_FOREGROUND) applyTo "editor.brackets"
}
