package com.klyx.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily

fun String.toAnnotatedString(
    fontFamily: FontFamily = FontFamily.Default,
    color: Color = Color.Unspecified,
    background: Color = Color.Unspecified
) = AnnotatedString(
    text = this,
    spanStyle = SpanStyle(
        fontFamily = fontFamily,
        color = color,
        background = background
    )
)
