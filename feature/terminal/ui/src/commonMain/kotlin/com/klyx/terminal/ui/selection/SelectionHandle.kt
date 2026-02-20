package com.klyx.terminal.ui.selection

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.klyx.terminal.ui.selection.icon.TextSelectHandleLeft
import com.klyx.terminal.ui.selection.icon.TextSelectHandleRight

@Composable
internal fun SelectionHandle(
    state: HandleState,
    containerBounds: ContainerBounds,
    containerPaddingPx: Float = 0f,
    onUpdatePosition: (screenX: Int, screenY: Int) -> Unit,
    onDragStarted: (() -> Unit)? = null,
    onDragEnded: (() -> Unit)? = null,
) {
    if (!state.isVisible && !state.isDragging) return

    val density = LocalDensity.current
    val handleSizeDp: DpSize = remember(state.handleWidthPx, state.handleHeightPx) {
        with(density) {
            DpSize(
                width = state.handleWidthPx.toDp(),
                height = state.handleHeightPx.toDp(),
            )
        }
    }

    val clipLeft = containerBounds.left + containerPaddingPx
    val clipTop = containerBounds.top + containerPaddingPx
    val clipRight = containerBounds.right - containerPaddingPx
    val clipBottom = containerBounds.bottom - containerPaddingPx

    Popup(
        offset = state.popupOffset,
        properties = PopupProperties(
            focusable = false,
            clippingEnabled = false,
            dismissOnBackPress = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .size(handleSizeDp)
                .pointerInput(state) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        val rawXAtDown = state.pointX + down.position.x
                        val rawYAtDown = state.pointY + down.position.y

                        state.onDragStart(
                            rawX = rawXAtDown,
                            rawY = rawYAtDown,
                            parentX = containerBounds.left.toInt(),
                            parentY = containerBounds.top.toInt(),
                        )
                        onDragStarted?.invoke()

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: break

                            if (!change.pressed) {
                                state.onDragEnd()
                                onDragEnded?.invoke()
                                break
                            }

                            val delta = change.positionChange()
                            if (delta != Offset.Zero) {
                                change.consume()

                                val rawX = state.pointX + change.position.x
                                val rawY = state.pointY + change.position.y

                                state.onParentMoved(
                                    currentParentX = containerBounds.left.toInt(),
                                    currentParentY = containerBounds.top.toInt(),
                                )

                                state.checkOrientation(
                                    posX = state.pointX,
                                    clipLeft = clipLeft,
                                    clipRight = clipRight,
                                )

                                val (newX, newY) = state.onDragMove(rawX, rawY)
                                onUpdatePosition(newX, newY)
                            }
                        }
                    }
                }
        ) {
            Image(
                imageVector = when (state.orientation) {
                    HandleOrientation.Left -> TextSelectHandleLeft
                    HandleOrientation.Right -> TextSelectHandleRight
                },
                contentDescription = if (state.initialOrientation == HandleOrientation.Left) {
                    "Selection start handle"
                } else {
                    "Selection end handle"
                },
                modifier = Modifier.size(handleSizeDp),
            )
        }
    }
}
