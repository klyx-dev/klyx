package com.klyx.editor

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.editor.cursor.CURSOR_BLINK_RATE
import com.klyx.editor.input.codeEditorInput

@Composable
@ExperimentalCodeEditorApi
fun CodeEditor(
    modifier: Modifier = Modifier,
    state: CodeEditorState = rememberCodeEditorState(),
    fontFamily: FontFamily = FontFamily.Monospace,
) {
    val textToolbar = LocalTextToolbar.current
    val haptics = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    val transition = rememberInfiniteTransition()
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CURSOR_BLINK_RATE, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CursorAlpha"
    )

    val focusRequester = remember { FocusRequester() }

    CodeEditorCanvas(
        modifier = modifier
            .sizeIn(minWidth = 100.dp, minHeight = 100.dp)
            .focusRequester(focusRequester)
            .codeEditorInput(
                state = state,
                keyboardController = keyboardController
            )
            .focusable(interactionSource = remember { MutableInteractionSource() })
            .pointerInput(state) {
                detectTapGestures(
                    onTap = {
                        focusRequester.requestFocus()

                        if (textToolbar.status == TextToolbarStatus.Shown) {
                            textToolbar.hide()
                        }
                    },
                    onLongPress = { offset ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                        textToolbar.showMenu(
                            rect = Rect(offset, Size.Zero),
                            onCopyRequested = state::copyText,
                            onPasteRequested = state::paste
                        )
                    }
                )
            }
    ) {
        drawRect(Color.White)

        val result = textMeasurer.measure(
            text = state.text,
            style = TextStyle(
                fontFamily = fontFamily,
                fontSize = 18.sp,
                //letterSpacing = 1.em
            ),
            softWrap = true,
            constraints = Constraints.fixed(size.width.toInt(), size.height.toInt())
        )

        drawText(
            textLayoutResult = result,
            brush = Brush.linearGradient(
                listOf(
                    Color.Red,
                    Color.Blue,
                    Color.Green
                )
            )
        )

        val cursorRect = result.getCursorRect(state.cursorPosition.offset)
        println(cursorRect.size)

        drawRect(
            brush = Brush.linearGradient(
                listOf(
                    Color.Red,
                    Color.Blue,
                    Color.Green
                )
            ),
            alpha = cursorAlpha,
            topLeft = cursorRect.topLeft,
            size = cursorRect.size.copy(width = 2.dp.toPx())
        )

        val selectionRange = state.getResolvedSelectionRange()

        drawPath(
            path = result.getPathForRange(
                start = selectionRange.start,
                end = selectionRange.end
            ),
            color = Color.Yellow,
            alpha = 0.5f
        )
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
