package com.klyx.editor.treesitter

import android.content.Context
import io.github.rosemoe.sora.lang.styling.textStyle
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.treesitter.ktreesitter.Language

fun createEditorLanguage(
    context: Context,
    languageName: String,
    language: Any,
    languageProvider: LanguageProvider,
    themeOverrides: TsThemeBuilder.() -> Unit = {},
): EditorLanguage {
    val tsLanguage = Language(language)

    return EditorLanguage(
        tsLanguage = tsLanguage,
        queries = { language ->
            LanguageQueries(context, language, languageName)
        },
        languageProvider = languageProvider,
        themeDescription = {
            editorTheme()
            themeOverrides()
        }
    )
}

fun createEditorLanguage(
    languageName: String,
    languagePointer: Long,
    querySource: String,
    indentsSource: String? = null,
    foldsSource: String? = null,
    localsSource: String? = null,
    injectionsSource: String? = null,
    tagsSource: String? = null,
    languageProvider: LanguageProvider,
    themeOverrides: TsThemeBuilder.() -> Unit = {},
): EditorLanguage {
    val tsLanguage = Language(languagePointer)
    val queries = LanguageQueries.fromSource(
        language = tsLanguage,
        languageName = languageName,
        highlights = querySource,
        indents = indentsSource,
        folds = foldsSource,
        locals = localsSource,
        injections = injectionsSource,
        tags = tagsSource,
    )

    return EditorLanguage(
        tsLanguage = tsLanguage,
        queries = { queries },
        languageProvider = languageProvider,
        themeDescription = {
            editorTheme()
            themeOverrides()
        }
    )
}

fun TsThemeBuilder.editorTheme() {
    // Keywords & Modifiers
    textStyle(
        EditorColorScheme.KEYWORD,
        bold = true
    ) applyTo arrayOf(
        "keyword",
        "keyword.function",
        "keyword.operator",
        "keyword.directive",
        "keyword.import",
        "keyword.storage",
        "keyword.repeat",
        "keyword.return",
        "keyword.exception",
        "conditional",
        "repeat",
        "label",
        "exception",
        "include",
        "storage",
        "modifier"
    )

    // Types, Interfaces & Structural Declarations
    textStyle(
        EditorColorScheme.IDENTIFIER_NAME
    ) applyTo arrayOf(
        "type",
        "type.builtin",
        "type.definition",
        "type.qualifier",
        "constructor",
        "class",
        "interface",
        "enum",
        "namespace",
        "module"
    )

    // Functions & Execution Blocks
    textStyle(
        EditorColorScheme.FUNCTION_NAME,
        italic = true
    ) applyTo arrayOf(
        "function",
        "function.call",
        "function.method",
        "function.builtin",
        "function.macro",
        "method",
        "method.call"
    )

    // Variables, Fields & Instance Scope Properties
    textStyle(
        EditorColorScheme.IDENTIFIER_VAR
    ) applyTo arrayOf(
        "variable",
        "variable.parameter",
        "variable.member",
        "variable.builtin",
        "property",
        "field",
        "parameter",
        "member"
    )

    // Raw Literals, Numerics & Boolean Primitives
    textStyle(
        EditorColorScheme.LITERAL
    ) applyTo arrayOf(
        "string",
        "string.escape",
        "string.regex",
        "string.special",
        "character",
        "character.special",
        "number",
        "number.float",
        "float",
        "constant",
        "constant.builtin",
        "boolean"
    )

    // Code Documentation & Internal Comments
    textStyle(
        EditorColorScheme.COMMENT,
        italic = true
    ) applyTo arrayOf(
        "comment",
        "comment.documentation",
        "comment.block"
    )

    // System Annotations, Decorators & Meta Attributes
    textStyle(
        EditorColorScheme.ANNOTATION
    ) applyTo arrayOf(
        "annotation",
        "attribute",
        "decorator",
        "meta.annotation"
    )

    // Arithmetic Operators, Punctuations & Brackets
    textStyle(
        EditorColorScheme.OPERATOR
    ) applyTo arrayOf(
        "operator",
        "delimiter",
        "punctuation",
        "punctuation.special",
        "punctuation.delimiter",
        "punctuation.bracket"
    )

    // HTML / XML Layout Structural Tags
    textStyle(
        EditorColorScheme.HTML_TAG,
        bold = true
    ) applyTo arrayOf(
        "tag",
        "tag.builtin",
        "tag.delimiter"
    )

    // HTML / XML Key Attributes
    textStyle(
        EditorColorScheme.ATTRIBUTE_NAME
    ) applyTo arrayOf(
        "attribute",
        "attribute.name"
    )

    // HTML / XML Embedded Value Chains
    textStyle(
        EditorColorScheme.ATTRIBUTE_VALUE
    ) applyTo arrayOf(
        "attribute.value"
    )

    // Structural Compiler Faults & Syntax Errors
    textStyle(
        EditorColorScheme.PROBLEM_ERROR
    ) applyTo "error"

    // Diagnostics Engine Warnings
    textStyle(
        EditorColorScheme.PROBLEM_WARNING
    ) applyTo "warning"
}
