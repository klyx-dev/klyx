package com.klyx.editor

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.viewinterop.AndroidView
import com.klyx.editor.language.JsonLanguage
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.widget.CodeEditor

@ExperimentalCodeEditorApi
private fun setCodeEditorFactory(
    context: Context,
    state: CodeEditorState
): CodeEditor {
    val editor = CodeEditor(context)
    editor.apply { setText(state.content) }
    state.editor = editor
    return editor
}

@Composable
@ExperimentalCodeEditorApi
actual fun CodeEditor(
    state: CodeEditorState,
    modifier: Modifier,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    editable: Boolean,
    pinLineNumber: Boolean,
    language: String?
) {
    val isDarkMode = isSystemInDarkTheme()
    val fontFamilyResolver = LocalFontFamilyResolver.current

    val style by remember {
        derivedStateOf {
            TextStyle(
                fontFamily = fontFamily,
                fontSize = fontSize
            )
        }
    }

    val typeface by fontFamilyResolver.resolveAsTypeface(style.fontFamily)

    val context = LocalContext.current
    val editor = remember(state) {
        setCodeEditorFactory(context, state)
    }

    LaunchedEffect(state.content) {
        state.editor?.setText(state.content)
    }

    AndroidView(
        factory = { editor },
        onRelease = { it.release() },
        modifier = modifier,
        update = { editor ->
            editor.apply {
                setTextSize(style.fontSize.value)
                typefaceText = typeface
                typefaceLineNumber = typeface

                setPinLineNumber(pinLineNumber)
                this.editable = editable
                this.colorScheme = DefaultColorScheme

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
