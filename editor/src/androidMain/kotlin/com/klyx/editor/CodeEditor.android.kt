package com.klyx.editor

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.event.ContentChangeEvent
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
    pinLineNumber: Boolean
) {
    val isDarkMode = isSystemInDarkTheme()
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val colorScheme = MaterialTheme.colorScheme

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
                        setColor(WHOLE_BACKGROUND, colorScheme.background.toArgb())
                        setColor(TEXT_NORMAL, colorScheme.onSurface.toArgb())
                        setColor(LINE_NUMBER_BACKGROUND, colorScheme.surface.toArgb())
                        setColor(LINE_NUMBER, colorScheme.onSurface.toArgb())
                        setColor(LINE_NUMBER_CURRENT, colorScheme.onSurface.toArgb())
                        setColor(LINE_DIVIDER, colorScheme.outline.toArgb())
                        setColor(CURRENT_LINE, colorScheme.surfaceContainerLow.toArgb())
                        setColor(SCROLL_BAR_THUMB, colorScheme.surfaceVariant.toArgb())
                        setColor(
                            SCROLL_BAR_THUMB_PRESSED,
                            colorScheme.surfaceColorAtElevation(5.dp).toArgb()
                        )
                        setColor(
                            SELECTED_TEXT_BACKGROUND,
                            colorScheme.primaryContainer.copy(alpha = 0.6f).toArgb()
                        )
                        setColor(SELECTION_HANDLE, colorScheme.primaryContainer.toArgb())
                        setColor(MATCHED_TEXT_BACKGROUND, getColor(SELECTED_TEXT_BACKGROUND))
                        setColor(
                            COMPLETION_WND_BACKGROUND,
                            colorScheme.surfaceColorAtElevation(6.dp).toArgb()
                        )
                        setColor(
                            COMPLETION_WND_TEXT_PRIMARY,
                            colorScheme.onSurface.toArgb()
                        )
                        setColor(
                            COMPLETION_WND_TEXT_SECONDARY,
                            colorScheme.onSurfaceVariant.toArgb()
                        )
                        setColor(COMPLETION_WND_ITEM_CURRENT, colorScheme.primary.toArgb())
                    }
                }
            }
        }
    )
}
