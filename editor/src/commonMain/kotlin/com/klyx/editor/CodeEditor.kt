package com.klyx.editor

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.editor.cursor.CURSOR_BLINK_RATE
import com.klyx.editor.cursor.CursorPosition
import com.klyx.editor.input.codeEditorInput
import kotlin.math.roundToInt

@Composable
@ExperimentalCodeEditorApi
fun CodeEditor(
    modifier: Modifier = Modifier,
    state: CodeEditorState = rememberCodeEditorState(),
    fontFamily: FontFamily = FontFamily.Monospace,
    fontSize: TextUnit = 18.sp
) {
    val haptics = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val colorScheme = MaterialTheme.colorScheme

    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    val transition = rememberInfiniteTransition()
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CURSOR_BLINK_RATE, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CursorAlpha"
    )

    LaunchedEffect(Unit) { state.startFpsTracker() }

    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier
            .imePadding()
    ) {
        CodeEditorCanvas(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight()
        ) {
            drawRect(colorScheme.surfaceContainerHigh)

            if (state.textLayoutResult != null) {
                for (line in 0 until state.lineCount) {
                    drawText(
                        textMeasurer.measure(
                            text = (line + 1).toString(),
                            style = TextStyle(
                                fontFamily = fontFamily,
                                fontSize = fontSize,
                                color = colorScheme.onSurface,
                                textAlign = TextAlign.End
                            ),
                            constraints = Constraints(maxWidth = 40.dp.roundToPx())
                        ),
                        topLeft = Offset(2f, state.getLineTop(line))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(2.dp))

        CodeEditorCanvas(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .codeEditorInput(
                    state = state,
                    keyboardController = keyboardController
                )
                .focusable(interactionSource = remember { MutableInteractionSource() })
                .pointerInput(state) {
                    detectTapGestures(
                        onTap = { position ->
                            focusRequester.requestFocus()
                            if (state.isTextSelected()) state.clearSelection()
                            state.hideTextToolbarIfShown()

                            state.cursorPosition = CursorPosition(
                                offset = state.getOffsetForPosition(position)
                            )
                        },
                        onLongPress = { position ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                            val offset = state.getOffsetForPosition(position)
                            val wordBoundary = state.getWordBoundary(offset)
                            state.select(wordBoundary)
                            state.cursorPosition = CursorPosition(wordBoundary.end)

                            state.showTextToolbar(position)
                        }
                    )
                }
        ) {
            drawRect(colorScheme.background)

            val result = textMeasurer.measure(
                text = state.text,
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = fontSize,
                    color = colorScheme.onSurface
                    //fontWeight = FontWeight.Bold,
                    //letterSpacing = 1.em
                ),
                constraints = Constraints(
                    maxWidth = size.width.roundToInt()
                ),
                softWrap = false
            )
            state.textLayoutResult = result

            drawText(result)

            val cursorRect = state.getCursorRect()
            //println(cursorRect.size)

            drawLine(
                color = colorScheme.primary,
                alpha = cursorAlpha,
                start = cursorRect.topCenter,
                end = cursorRect.bottomCenter,
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )

            drawPath(
                path = state.getPathForSelectionRange(),
                color = colorScheme.primaryContainer,
                alpha = 0.5f
            )
        }
    }
}

@Composable
private fun CodeEditorCanvas(
    modifier: Modifier = Modifier,
    onDraw: DrawScope.() -> Unit
) {
    Layout(
        measurePolicy = CodeEditorMeasurePolicy,
        modifier = modifier.drawBehind(onDraw)
    )
}

private object CodeEditorMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        return with(constraints) {
            val width = if (hasFixedWidth) maxWidth else 0
            val height = if (hasFixedHeight) maxHeight else 0
            layout(width, height) {}
        }
    }
}

@Stable
fun Size.toConstraints() = Constraints(maxWidth = width.toInt(), maxHeight = height.toInt())
