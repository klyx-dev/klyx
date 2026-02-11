package com.klyx.terminal.ui.selection

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.klyx.terminal.ui.FontMetrics
import com.klyx.terminal.ui.TerminalState

@Composable
private fun TextSelectionHandle(
    visible: Boolean,
    orientation: HandleOrientation,
    position: HandlePosition,
    onDrag: (Offset) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    handleScale: Float = 0.7f,
    brush: Brush = SolidColor(Color(0xFF2196F3))
) {
    if (!visible) return

    Canvas(
        modifier = modifier
            .offset {
                IntOffset(
                    x = position.x,
                    y = position.y
                )
            }
            .size(48.dp, 24.dp)
            .zIndex(1000f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        println(dragAmount)
                        onDrag(dragAmount)
                    }
                )
            }
    ) {
        withTransform({
            scale(handleScale, handleScale)
        }) {
            drawPath(
                path = orientation.createPath(),
                brush = brush,
            )
        }
    }
}

internal fun HandleOrientation.createPath(): Path {
    return when (this) {
        HandleOrientation.Left -> PathData {
            moveTo(52.3f, 1.6f)
            curveToRelative(-5.7f, 2.1f, -12.9f, 8.6f, -16f, 14.8f)
            curveToRelative(-2.2f, 4.1f, -2.8f, 6.9f, -3.1f, 14.3f)
            curveToRelative(-0.6f, 12.6f, 1.3f, 17.8f, 9.3f, 25.8f)
            curveToRelative(8f, 8f, 13.2f, 9.9f, 25.8f, 9.3f)
            curveToRelative(11.1f, -0.5f, 17.3f, -3.2f, 23.5f, -10.3f)
            curveToRelative(6.5f, -7.4f, 7.2f, -10.8f, 7.2f, -34.7f)
            lineToRelative(0f, -20.8f)
            lineToRelative(-21.2f, 0.1f)
            curveToRelative(-16.1f, -0f, -22.3f, 0.4f, -25.5f, 1.5f)
            close()
        }

        HandleOrientation.Right -> PathData {
            moveTo(33f, 20.8f)
            curveToRelative(0f, 23.9f, 0.7f, 27.3f, 7.2f, 34.7f)
            curveToRelative(6.2f, 7.1f, 12.4f, 9.8f, 23.5f, 10.3f)
            curveToRelative(12.6f, 0.6f, 17.8f, -1.3f, 25.8f, -9.3f)
            curveToRelative(8f, -8f, 9.9f, -13.2f, 9.3f, -25.8f)
            curveToRelative(-0.5f, -11.1f, -3.2f, -17.3f, -10.3f, -23.5f)
            curveToRelative(-7.4f, -6.5f, -10.8f, -7.2f, -34.7f, -7.2f)
            lineToRelative(-20.8f, -0f)
            lineToRelative(0f, 20.8f)
            close()
        }
    }.toPath()
}

@Composable
private fun TextSelectionHandles(
    controller: SelectionController,
    metrics: FontMetrics,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        TextSelectionHandle(
            visible = controller.isActive,
            orientation = controller.startHandleOrientation,
            position = controller.startHandlePosition,
            onDragStart = controller::onStartHandleDragStart,
            onDrag = { dragAmount ->
                controller.onStartHandleDrag(dragAmount, metrics)
            },
            onDragEnd = controller::onStartHandleDragEnd,
        )

        TextSelectionHandle(
            visible = controller.isActive,
            orientation = controller.endHandleOrientation,
            position = controller.endHandlePosition,
            onDragStart = controller::onEndHandleDragStart,
            onDrag = { dragAmount ->
                controller.onEndHandleDrag(dragAmount, metrics)
            },
            onDragEnd = controller::onEndHandleDragEnd,
        )
    }
}

@Composable
internal fun TextSelectionOverlay(
    state: TerminalState,
    controller: SelectionController,
    metrics: FontMetrics,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(
        state.selectionX1.intValue,
        state.selectionY1.intValue,
        state.selectionX2.intValue,
        state.selectionY2.intValue,
        state.topRow.intValue
    ) {
        if (controller.isActive) {
            controller.render(metrics)
        }
    }

    TextSelectionHandles(
        controller = controller,
        metrics = metrics,
        modifier = modifier
    )
}

