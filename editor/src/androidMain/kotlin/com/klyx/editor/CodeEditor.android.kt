package com.klyx.editor

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import com.klyx.editor.language.JsonLanguage
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.subscribeAlways

@Composable
@ExperimentalCodeEditorApi
actual fun CodeEditor(
    modifier: Modifier,
    state: CodeEditorState,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    editable: Boolean,
    pinLineNumber: Boolean,
    language: String?
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val textToolbar = LocalTextToolbar.current

    LaunchedEffect(state) {
        state.coroutineScope = scope
        state.textToolbar = textToolbar
        state.clipboard = clipboard
    }

    val isDarkMode = isSystemInDarkTheme()
    val fontFamilyResolver = LocalFontFamilyResolver.current

    val style by remember {
        derivedStateOf {
            TextStyle(
                fontFamily = fontFamily, fontSize = fontSize
            )
        }
    }

    val typeface by fontFamilyResolver.resolveAsTypeface(style.fontFamily)

    AndroidView(
        factory = {
            CodeEditor(it).apply {
                setText(state.text)

                subscribeAlways<ContentChangeEvent> {
                    state.setText(it.editor.text.toString())
                }
            }
        },
        onRelease = { it.release() },
        modifier = modifier,
        update = { editor ->
            editor.apply {
                setTextSize(style.fontSize.value)
                typefaceText = typeface
                typefaceLineNumber = typeface

                setPinLineNumber(pinLineNumber)
                this.editable = editable

                this.colorScheme = object : EditorColorScheme(isDarkMode) {
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

                setEditorLanguage(
                    when (language) {
                        "json" -> JsonLanguage()
                        else -> EmptyLanguage()
                    }
                )
            }
        }
    )
}
