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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.zIndex
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
    fontSize: TextUnit = 18.sp,
    editable: Boolean = true,
    pinLineNumber: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
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
    var gutterWidth by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
    ) {
        CodeEditorCanvas(
            modifier = Modifier
                .width(with(density) { gutterWidth.toDp() })
                .fillMaxHeight()
                .zIndex(100f)
        ) {
            gutterWidth = 40.dp.toPx()

            translate(
                left = if (pinLineNumber) 0f else state.scrollX
            ) {
                clipRect {
                    drawRect(
                        color = colorScheme.surfaceContainerHigh,
                        size = size
                    )

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
                                    constraints = Constraints(maxWidth = gutterWidth.roundToInt())
                                ),
                                topLeft = Offset(2f, state.getLineTop(line) + state.scrollY)
                            )
                        }
                    }
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
                    editable = editable,
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
                                offset = state.getOffsetForPosition(position - state.scrollOffset)
                            )
                        },
                        onLongPress = { position ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                            val offset = state.getOffsetForPosition(position - state.scrollOffset)
                            val wordBoundary = state.getWordBoundary(offset)
                            state.select(wordBoundary)
                            state.cursorPosition = CursorPosition(wordBoundary.end)

                            state.showTextToolbar(position, editable)
                        }
                    )
                }
                .codeEditorScroll(state)
        ) {
            state.canvasSize = size

            clipRect(
                left = if (pinLineNumber) 0f else state.scrollX
            ) {
                drawRect(colorScheme.background)

                val result = textMeasurer.measure(
                    text = state.text,
                    style = TextStyle(
                        fontFamily = fontFamily,
                        fontSize = fontSize,
                        color = colorScheme.onSurface
                    ),
                    softWrap = false
                )
                state.textLayoutResult = result

                drawText(
                    textLayoutResult = result,
                    topLeft = state.scrollOffset
                )

                if (editable) {
                    val cursorRect = state.getCursorRect()

                    drawLine(
                        color = colorScheme.primary,
                        alpha = cursorAlpha,
                        start = cursorRect.topCenter + state.scrollOffset,
                        end = cursorRect.bottomCenter + state.scrollOffset,
                        strokeWidth = 2f,
                        cap = StrokeCap.Round,
                    )
                }

                drawPath(
                    path = state.getPathForSelectionRange().apply { translate(state.scrollOffset) },
                    color = colorScheme.primaryContainer,
                    alpha = 0.5f,
                )
            }
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
