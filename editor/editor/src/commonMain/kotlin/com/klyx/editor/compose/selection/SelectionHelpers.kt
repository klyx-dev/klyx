package com.klyx.editor.compose.selection

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.absoluteValue
import kotlin.math.sign

internal fun TextLayoutResult.getTextDirectionForOffset(offset: Int): ResolvedTextDirection =
    if (isOffsetAnEmptyLine(offset)) getParagraphDirection(offset) else getBidiRunDirection(offset)

internal fun TextLayoutResult.isOffsetAnEmptyLine(offset: Int): Boolean =
    layoutInput.text.isEmpty() ||
            getLineForOffset(offset).let { currentLine ->
                // verify the previous and next offsets either don't exist because they're at a boundary
                // or that they are different lines than the current line.
                (offset == 0 || currentLine != getLineForOffset(offset - 1)) &&
                        (offset == layoutInput.text.length || currentLine != getLineForOffset(offset + 1))
            }

/** Returns whether the given pixel position is inside the selection. */
internal fun TextLayoutResult.isPositionInsideSelection(
    position: Offset,
    selectionRange: TextRange?,
): Boolean {
    if ((selectionRange == null) || selectionRange.collapsed) return false

    fun isOffsetSelectedAndContainsPosition(offset: Int) =
        selectionRange.contains(offset) && getBoundingBox(offset).contains(position)

    // getOffsetForPosition returns the index at which the cursor should be placed when the
    // given position is clicked. This means that when position is to the right of the center of
    // a glyph it will return the index of the next glyph. So we test both the index it returns
    // and the previous index.
    val offset = getOffsetForPosition(position)
    return isOffsetSelectedAndContainsPosition(offset) ||
            isOffsetSelectedAndContainsPosition(offset - 1)
}

internal fun Offset.coerceIn(rect: Rect): Offset {
    val xOffset =
        when {
            x < rect.left -> rect.left
            x > rect.right -> rect.right
            else -> x
        }
    val yOffset =
        when {
            y < rect.top -> rect.top
            y > rect.bottom -> rect.bottom
            else -> y
        }
    return Offset(xOffset, yOffset)
}

internal suspend fun AwaitPointerEventScope.awaitPrimaryFirstDown(
    requireUnconsumed: Boolean = true,
    pass: PointerEventPass = PointerEventPass.Main,
): PointerInputChange {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(pass)
    } while (!event.isChangedToDown(requireUnconsumed, onlyPrimaryMouseButton = true))
    return event.changes[0]
}

/**
 * Whether [AwaitPointerEventScope.awaitFirstDown], for mouse events, responds only to the primary
 * mouse button being pressed. The behavior currently differs between Android and Desktop, and
 * eventually this needs to be aligned (b/384562201).
 */
internal expect fun firstDownRefersToPrimaryMouseButtonOnly(): Boolean

internal fun PointerEvent.isChangedToDown(
    requireUnconsumed: Boolean,
    onlyPrimaryMouseButton: Boolean = firstDownRefersToPrimaryMouseButtonOnly(),
): Boolean {
    val onlyPrimaryButtonCausesDown =
        onlyPrimaryMouseButton && changes.fastAll { it.type == PointerType.Mouse }
    if (onlyPrimaryButtonCausesDown && !buttons.isPrimaryPressed) return false

    return changes.fastAll {
        if (requireUnconsumed) it.changedToDown() else it.changedToDownIgnoreConsumed()
    }
}

/**
 * Returns `true` if the current state of the pointer events has all pointers up and `false` if any
 * of the pointers are down.
 */
internal fun AwaitPointerEventScope.allPointersUp(): Boolean =
    !currentEvent.changes.fastAny { it.pressed }

internal suspend fun AwaitPointerEventScope.awaitAllPointersUpWithSlopDetection(
    initialPositionChange: PointerInputChange,
    pass: PointerEventPass = PointerEventPass.Main,
): Boolean {
    if (allPointersUp()) {
        return false
    }

    var pointer: PointerId = initialPositionChange.id
    var pointerSlopReached = false
    val touchSlop = viewConfiguration.pointerSlop(initialPositionChange.type)
    val touchSlopDetector = TouchSlopDetector()
    do {
        val event = awaitPointerEvent(pass)
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer }
        if (dragEvent == null || dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return pointerSlopReached
            } else {
                pointer = otherDown.id
            }
        } else {
            val postSlopOffset =
                touchSlopDetector.addPositions(
                    dragEvent.position,
                    dragEvent.previousPosition,
                    touchSlop,
                )
            if (postSlopOffset.isSpecified) {
                pointerSlopReached = true
            }
        }
    } while (event.changes.fastAny { it.pressed })
    return pointerSlopReached
}

/**
 * Detects if touch slop has been crossed after adding a series of [PointerInputChange]. For every
 * new [PointerInputChange] one should add it to this detector using [addPositions]. If the position
 * change causes the touch slop to be crossed, [addPositions] will return true.
 */
internal class TouchSlopDetector(
    var orientation: Orientation? = null,
    initialPositionChange: Offset = Offset.Zero,
) {

    fun Offset.mainAxis() = if (orientation == Orientation.Horizontal) x else y

    fun Offset.crossAxis() = if (orientation == Orientation.Horizontal) y else x

    /** The accumulation of drag deltas in this detector. */
    private var totalPositionChange: Offset = initialPositionChange

    /**
     * Adds [dragEvent] to this detector. If the accumulated position changes crosses the touch slop
     * provided by [touchSlop], this method will return the post slop offset, that is the total
     * accumulated delta change minus the touch slop value, otherwise this should return null.
     */
    fun addPositions(currentPosition: Offset, previousPosition: Offset, touchSlop: Float): Offset {
        val positionChange = currentPosition - previousPosition
        totalPositionChange += positionChange

        val inDirection =
            if (orientation == null) {
                totalPositionChange.getDistance()
            } else {
                totalPositionChange.mainAxis().absoluteValue
            }

        val hasCrossedSlop = inDirection >= touchSlop

        return if (hasCrossedSlop) {
            calculatePostSlopOffset(touchSlop)
        } else {
            Offset.Unspecified
        }
    }

    /**
     * Resets the accumulator associated with this detector.
     *
     * @param initialPositionAccumulator Use to initialize the position change accumulator, for
     *   instance in cases where slop detection may happen "mid-gesture", that is, the slop
     *   detection didn't start from the first down event but somewhere after.
     */
    fun reset(initialPositionAccumulator: Offset = Offset.Zero) {
        totalPositionChange = initialPositionAccumulator
    }

    private fun calculatePostSlopOffset(touchSlop: Float): Offset {
        return if (orientation == null) {
            val touchSlopOffset =
                totalPositionChange / totalPositionChange.getDistance() * touchSlop
            // update postSlopOffset
            totalPositionChange - touchSlopOffset
        } else {
            val finalMainAxisChange =
                totalPositionChange.mainAxis() - (sign(totalPositionChange.mainAxis()) * touchSlop)
            val finalCrossAxisChange = totalPositionChange.crossAxis()
            if (orientation == Orientation.Horizontal) {
                Offset(finalMainAxisChange, finalCrossAxisChange)
            } else {
                Offset(finalCrossAxisChange, finalMainAxisChange)
            }
        }
    }
}
