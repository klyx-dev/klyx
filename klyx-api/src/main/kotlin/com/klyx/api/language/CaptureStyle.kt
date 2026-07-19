package com.klyx.api.language

data class CaptureStyle(
    val editorColorKey: ColorKey = ColorKey.TEXT_NORMAL,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strikethrough: Boolean = false,
) {
    companion object {
        val KEYWORD = CaptureStyle(editorColorKey = ColorKey.KEYWORD, bold = true)
        val TYPE = CaptureStyle(editorColorKey = ColorKey.IDENTIFIER_NAME)
        val FUNCTION = CaptureStyle(editorColorKey = ColorKey.FUNCTION_NAME, italic = true)
        val VARIABLE = CaptureStyle(editorColorKey = ColorKey.IDENTIFIER_VAR)
        val STRING = CaptureStyle(editorColorKey = ColorKey.LITERAL)
        val COMMENT = CaptureStyle(editorColorKey = ColorKey.COMMENT, italic = true)
        val ANNOTATION = CaptureStyle(editorColorKey = ColorKey.ANNOTATION)
        val OPERATOR = CaptureStyle(editorColorKey = ColorKey.OPERATOR)
        val TAG = CaptureStyle(editorColorKey = ColorKey.HTML_TAG, bold = true)
        val ATTRIBUTE_NAME = CaptureStyle(editorColorKey = ColorKey.ATTRIBUTE_NAME)
        val ATTRIBUTE_VALUE = CaptureStyle(editorColorKey = ColorKey.ATTRIBUTE_VALUE)
        val ERROR = CaptureStyle(editorColorKey = ColorKey.PROBLEM_ERROR)
        val WARNING = CaptureStyle(editorColorKey = ColorKey.PROBLEM_WARNING)
    }
}

enum class ColorKey {
    KEYWORD,
    IDENTIFIER_NAME,
    FUNCTION_NAME,
    IDENTIFIER_VAR,
    LITERAL,
    COMMENT,
    ANNOTATION,
    OPERATOR,
    HTML_TAG,
    ATTRIBUTE_NAME,
    ATTRIBUTE_VALUE,
    PROBLEM_ERROR,
    PROBLEM_WARNING,
    TEXT_NORMAL,
}
