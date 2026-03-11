package com.klyx.terminal.ui.selection

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import kotlin.math.roundToInt

@Composable
internal fun SelectionHandle(
    orientation: HandleOrientation,
    anchorX: Float,
    anchorY: Float,
    leftIcon: ImageVector,
    rightIcon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    onDragStart: () -> Unit = {},
    onDragPosition: (pixelX: Float, pixelY: Float) -> Unit,
    onDragEnd: () -> Unit = {},
) {
    var iconSize by remember { mutableStateOf(IntSize(48, 48)) }

    val hotspotX = when (orientation) {
        HandleOrientation.LEFT -> iconSize.width * 3f / 4f
        HandleOrientation.RIGHT -> iconSize.width * 1f / 4f
    }
    val hotspotY = 0f
    // pull the handle up slightly so the pointer tip aligns with the baseline
    val touchOffsetY = -iconSize.height * 0.3f

    // position the top-left of the icon so that the hot-spot lands on (anchorX, anchorY)
    val drawX = (anchorX - hotspotX).roundToInt()
    val drawY = (anchorY + touchOffsetY - hotspotY).roundToInt()

    // track accumulated drag so we can emit absolute positions
    var dragOriginX by remember { mutableStateOf(0f) }
    var dragOriginY by remember { mutableStateOf(0f) }
    var cumulativeDx by remember { mutableStateOf(0f) }
    var cumulativeDy by remember { mutableStateOf(0f) }

    val currentOrientation = rememberFlippedOrientation(
        anchorX = anchorX,
        initial = orientation,
        iconWidth = iconSize.width,
    )
    val icon = if (currentOrientation == HandleOrientation.LEFT) leftIcon else rightIcon

    Popup {
        Box(
            modifier = modifier
                .customOffset { IntOffset(drawX, drawY) }
                .onSizeChanged { iconSize = it }
                .pointerInput(anchorX, anchorY) {
                    detectDragGestures(
                        onDragStart = {
                            dragOriginX = anchorX
                            dragOriginY = anchorY
                            cumulativeDx = 0f
                            cumulativeDy = 0f
                            onDragStart()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            cumulativeDx += dragAmount.x
                            cumulativeDy += dragAmount.y
                            val newPixelX = dragOriginX + cumulativeDx - hotspotX + hotspotX
                            val newPixelY = dragOriginY + cumulativeDy + touchOffsetY + hotspotY
                            onDragPosition(newPixelX, newPixelY)
                        },
                        onDragEnd = {
                            cumulativeDx = 0f; cumulativeDy = 0f
                            onDragEnd()
                        },
                        onDragCancel = {
                            cumulativeDx = 0f; cumulativeDy = 0f
                            onDragEnd()
                        },
                    )
                }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (orientation == HandleOrientation.LEFT) "Selection start handle" else "Selection end handle",
                tint = tint,
            )
        }
    }
}

@Composable
private fun rememberFlippedOrientation(
    anchorX: Float,
    initial: HandleOrientation,
    iconWidth: Int,
): HandleOrientation {
    // The parent Layout provides its own width via onGloballyPositioned, but
    // for a lightweight solution we just track via the state exposed here and
    // let the caller re-compose when anchorX changes.
    return when {
        anchorX - iconWidth < 0 -> HandleOrientation.RIGHT
        // no right-edge guard without parent width. keep initial otherwise
        else -> initial
    }
}

private fun Modifier.customOffset(offset: () -> IntOffset): Modifier =
    this.then(Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            val off = offset()
            placeable.placeRelative(off.x, off.y)
        }
    })
