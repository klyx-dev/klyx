package com.klyx.editor

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.klyx.core.noLocalProvidedFor

internal val LocalTextMeasurer = compositionLocalOf<TextMeasurer> {
    noLocalProvidedFor<TextMeasurer>()
}

internal val LocalEditorTextStyle = compositionLocalOf { TextStyle.Default }
internal val LocalAppColorScheme = compositionLocalOf<ColorScheme> {
    noLocalProvidedFor<ColorScheme>()
}

@Composable
@ExperimentalCodeEditorApi
expect fun CodeEditor(
    modifier: Modifier = Modifier,
    state: CodeEditorState = rememberCodeEditorState(),
    fontFamily: FontFamily = FontFamily.Monospace,
    fontSize: TextUnit = 18.sp,
    editable: Boolean = true,
    pinLineNumber: Boolean = true,
    language: String? = null
)
