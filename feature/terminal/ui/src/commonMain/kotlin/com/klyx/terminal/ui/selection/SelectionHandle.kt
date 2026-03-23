package com.klyx.terminal.ui.selection

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

@Composable
internal fun SelectionHandle(
    orientation: HandleOrientation,
    anchorX: Float,
    anchorY: Float,
    maxWidth: Float,
    leftIcon: ImageVector,
    rightIcon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    onDragStart: () -> Unit = {},
    onDragPosition: (pixelX: Float, pixelY: Float) -> Unit,
    onDragEnd: () -> Unit = {},
) {
    var iconSize by remember { mutableStateOf(IntSize(48, 48)) }

    val currentOrientation = rememberFlippedOrientation(
        anchorX = anchorX,
        initial = orientation,
        iconWidth = iconSize.width,
        maxWidth = maxWidth
    )

    val icon = if (currentOrientation == HandleOrientation.LEFT) leftIcon else rightIcon

    val hotspotX = when (currentOrientation) {
        HandleOrientation.LEFT -> iconSize.width * 0.75f
        HandleOrientation.RIGHT -> iconSize.width * 0.25f
    }

    val drawX = (anchorX - hotspotX).roundToInt()
    val drawY = anchorY.roundToInt()

    var dragOriginX by remember { mutableStateOf(0f) }
    var dragOriginY by remember { mutableStateOf(0f) }
    var cumulativeDx by remember { mutableStateOf(0f) }
    var cumulativeDy by remember { mutableStateOf(0f) }

    val currentAnchorX by rememberUpdatedState(anchorX)
    val currentAnchorY by rememberUpdatedState(anchorY)
    val currentOnDragPosition by rememberUpdatedState(onDragPosition)

    Box(
        modifier = modifier
            .offset { IntOffset(drawX, drawY) }
            .onSizeChanged { iconSize = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragOriginX = currentAnchorX
                        dragOriginY = currentAnchorY
                        cumulativeDx = 0f
                        cumulativeDy = 0f
                        onDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        cumulativeDx += dragAmount.x
                        cumulativeDy += dragAmount.y
                        currentOnDragPosition(dragOriginX + cumulativeDx, dragOriginY + cumulativeDy)
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
            contentDescription = "Selection handle",
            tint = tint,
        )
    }
}

@Composable
private fun rememberFlippedOrientation(
    anchorX: Float,
    initial: HandleOrientation,
    iconWidth: Int,
    maxWidth: Float
): HandleOrientation {
    return when (initial) {
        HandleOrientation.LEFT if (anchorX - iconWidth * 0.75f) < 0 -> HandleOrientation.RIGHT
        HandleOrientation.RIGHT if (anchorX + iconWidth * 0.25f) > maxWidth -> HandleOrientation.LEFT
        else -> initial
    }
}
