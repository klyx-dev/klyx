package com.klyx.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.subscribeAlways

@Composable
@ExperimentalCodeEditorApi
actual fun CodeEditor(
    modifier: Modifier,
    state: CodeEditorState,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    editable: Boolean,
    pinLineNumber: Boolean
) {
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
            }
        }
    )
}
