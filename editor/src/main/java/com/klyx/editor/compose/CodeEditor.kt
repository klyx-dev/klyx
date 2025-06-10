package com.klyx.editor.compose

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.klyx.core.rememberTypefaceFromFontFamily
import com.klyx.editor.CodeEditorState
import com.klyx.editor.cursor.toCursorPosition
import com.klyx.editor.theme.DefaultColorScheme
import com.klyx.editor.theme.KlyxColorScheme
import com.klyx.editor.language.KlyxLanguage
import com.klyx.editor.language.PlainLanguage
import com.klyx.editor.rememberCodeEditorState

@Composable
fun CodeEditor(
    modifier: Modifier = Modifier,
    editorState: CodeEditorState = rememberCodeEditorState(),
    language: KlyxLanguage = PlainLanguage,
    colorScheme: KlyxColorScheme = DefaultColorScheme,
    fontSize: TextUnit = 14.sp,
    lineSpacing: Dp = 0.dp,
    letterSpacing: TextUnit = 0.05.em,
    fontFamily: FontFamily = FontFamily.Monospace,
    lineNumberFontFamily: FontFamily = fontFamily,
    editable: Boolean = true,
    gutterWidth: Dp = 48.dp,
    pinLineNumber: Boolean = false
) {
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val typeface by rememberTypefaceFromFontFamily(fontFamily)
    val lineNumberTypeface by rememberTypefaceFromFontFamily(lineNumberFontFamily)

    val lineSpacingPx = remember { with(density) { lineSpacing.toPx() + calculateLineSpacing(fontSize).toPx() } }
    val gutterPadding = remember { with(density) { 8.dp.toPx() } }

    LaunchedEffect(language, colorScheme) {
        editorState.language = language
        editorState.colorScheme = colorScheme
    }

    val textPaint = remember(typeface, fontSize) {
        Paint().apply {
            isAntiAlias = true
            color = Color.White.toArgb()
            textSize = fontSize.value
            this.typeface = typeface
            this.letterSpacing = letterSpacing.value
        }
    }

    val lineNumberPaint = remember(lineNumberTypeface) {
        Paint().apply {
            isAntiAlias = true
            color = Color.Gray.toArgb()
            textSize = fontSize.value
            this.typeface = lineNumberTypeface
            this.letterSpacing = letterSpacing.value
            textAlign = Paint.Align.RIGHT
        }
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { editorState.viewportSize = it }
            .editorScroll(editorState, textPaint)
            .focusRequester(focusRequester)
            .editorTextInput(
                keyboardController = keyboardController,
                editorState = editorState
            )
            .focusable(interactionSource = remember { MutableInteractionSource() })
            .pointerInput(editorState) {
                detectTapGestures(onTap = { offset ->
                    if (!editable) return@detectTapGestures

                    focusRequester.requestFocus()

                    editorState.moveCursorTo(
                        offset.toCursorPosition(
                            editorState = editorState,
                            textPaint = textPaint
                        )
                    )
                })
            }
            .clipToBounds()
    ) {
        val scrollX = editorState.scrollOffset.x
        val scrollY = editorState.scrollOffset.y

        val fm = textPaint.fontMetrics
        val lineHeight = fm.descent - fm.ascent
        val lineHeightWithSpacing = lineHeight + lineSpacingPx

        editorState.lineHeight = lineHeight
        editorState.lineHeightWithSpacing = lineHeightWithSpacing
        editorState.gutterWidth = gutterWidth.toPx()
        editorState.gutterPadding = gutterPadding
        editorState.measureText = textPaint::measureText

        val baselineOffset = -fm.ascent

        val visibleLines = editorState.visibleLines

        // background
        drawRect(
            color = Color.Black,
            size = size
        )

        translate(left = gutterWidth.toPx() + scrollX, top = scrollY) {
            // text
            drawIntoCanvas { canvas ->
                visibleLines.fastForEach { (lineIndex, lineText) ->
                    val y = lineIndex * lineHeightWithSpacing + baselineOffset
                    canvas.nativeCanvas.drawText(lineText, gutterPadding, y, textPaint)
                }
            }

            // cursor
            if (editable) {
                val cursorPosition = editorState.cursorPosition
                val actualLine = cursorPosition.line - 1
                val currentLine = editorState.getLine(actualLine) ?: ""
                val textBeforeCursor = currentLine.substring(0, cursorPosition.column.coerceAtMost(currentLine.length))
                val cursorX = gutterPadding + textPaint.measureText(textBeforeCursor)

                drawLine(
                    color = Color.White,
                    strokeWidth = 1.dp.toPx(),
                    start = Offset(
                        x = cursorX,
                        y = actualLine * lineHeightWithSpacing
                    ),
                    end = Offset(
                        x = cursorX,
                        y = actualLine * lineHeightWithSpacing + lineHeight
                    )
                )
            }
        }

        translate(left = if (!pinLineNumber) scrollX else 0f) {
            // gutter
            drawRect(
                color = Color.DarkGray,
                size = Size(gutterWidth.toPx(), size.height)
            )

            // line number
            translate(top = scrollY) {
                drawIntoCanvas { canvas ->
                    visibleLines.fastForEach { (lineIndex, _) ->
                        val y = lineIndex * lineHeightWithSpacing + baselineOffset
                        canvas.nativeCanvas.drawText(
                            (lineIndex + 1).toString(),
                            gutterWidth.toPx() - gutterPadding,
                            y,
                            lineNumberPaint
                        )
                    }
                }
            }
        }
    }
}

private fun calculateLineSpacing(fontSize: TextUnit, multiplier: Float = 0.2f): Dp {
    return (fontSize.value * multiplier).dp
}
