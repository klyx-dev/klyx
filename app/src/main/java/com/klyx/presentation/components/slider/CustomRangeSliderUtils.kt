package com.klyx.presentation.components.slider

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Stable
internal fun Modifier.rangeSliderPressDragModifier(
    state: CustomRangeSliderState,
    startInteractionSource: MutableInteractionSource,
    endInteractionSource: MutableInteractionSource,
    enabled: Boolean
): Modifier = if (enabled) {
    pointerInput(startInteractionSource, endInteractionSource, state) {
        val rangeSliderLogic = RangeSliderLogic(
            state,
            startInteractionSource,
            endInteractionSource
        )
        coroutineScope {
            awaitEachGesture {
                val event = awaitFirstDown(requireUnconsumed = false)
                val interaction = DragInteraction.Start()
                var posX = if (state.isRtl)
                    state.totalWidth - event.position.x else event.position.x
                val compare = rangeSliderLogic.compareOffsets(posX)
                var draggingStart = if (compare != 0) {
                    compare < 0
                } else {
                    state.rawOffsetStart > posX
                }

                awaitSlop(event.id, event.type)?.let {
                    val slop = viewConfiguration.pointerSlop(event.type)
                    val shouldUpdateCapturedThumb = abs(state.rawOffsetEnd - posX) < slop &&
                            abs(state.rawOffsetStart - posX) < slop
                    if (shouldUpdateCapturedThumb) {
                        val dir = it.second
                        draggingStart = if (state.isRtl) dir >= 0f else dir < 0f
                        posX += it.first.positionChange().x
                    }
                }

                rangeSliderLogic.captureThumb(
                    draggingStart,
                    posX,
                    interaction,
                    this@coroutineScope
                )

                val finishInteraction = try {
                    val success = horizontalDrag(pointerId = event.id) {
                        val deltaX = it.positionChange().x
                        state.onDrag(draggingStart, if (state.isRtl) -deltaX else deltaX)
                    }
                    if (success) {
                        DragInteraction.Stop(interaction)
                    } else {
                        DragInteraction.Cancel(interaction)
                    }
                } catch (_: CancellationException) {
                    DragInteraction.Cancel(interaction)
                }

                state.gestureEndAction(draggingStart)
                launch {
                    rangeSliderLogic
                        .activeInteraction(draggingStart)
                        .emit(finishInteraction)
                }
            }
        }
    }
} else {
    this
}

internal suspend fun AwaitPointerEventScope.awaitSlop(
    id: PointerId,
    type: PointerType
): Pair<PointerInputChange, Float>? {
    var initialDelta = 0f
    val postPointerSlop = { pointerInput: PointerInputChange, offset: Float ->
        pointerInput.consume()
        initialDelta = offset
    }
    val afterSlopResult = awaitHorizontalPointerSlopOrCancellation(id, type, postPointerSlop)
    return if (afterSlopResult != null) afterSlopResult to initialDelta else null
}

internal class RangeSliderLogic(
    val state: CustomRangeSliderState,
    val startInteractionSource: MutableInteractionSource,
    val endInteractionSource: MutableInteractionSource
) {
    fun activeInteraction(draggingStart: Boolean): MutableInteractionSource =
        if (draggingStart) startInteractionSource else endInteractionSource

    fun compareOffsets(eventX: Float): Int {
        val diffStart = abs(state.rawOffsetStart - eventX)
        val diffEnd = abs(state.rawOffsetEnd - eventX)
        return diffStart.compareTo(diffEnd)
    }

    fun captureThumb(
        draggingStart: Boolean,
        posX: Float,
        interaction: Interaction,
        scope: CoroutineScope
    ) {
        state.onDrag(
            draggingStart,
            posX - if (draggingStart) state.rawOffsetStart else state.rawOffsetEnd
        )
        scope.launch {
            activeInteraction(draggingStart).emit(interaction)
        }
    }
}