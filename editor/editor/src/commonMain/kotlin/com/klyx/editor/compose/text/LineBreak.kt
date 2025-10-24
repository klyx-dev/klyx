package com.klyx.editor.compose.text

import kotlinx.serialization.Serializable

// line separator
@Serializable
enum class LineBreak {
    /** Use the end of line character identified in the text buffer. */
    TextDefined,

    /** Use line feed (\n) as the end of line character. */
    LF,

    /** Use carriage return and line feed (\r\n) as the end of line character. */
    CRLF,

    /** Invalid end of line */
    Invalid
}
