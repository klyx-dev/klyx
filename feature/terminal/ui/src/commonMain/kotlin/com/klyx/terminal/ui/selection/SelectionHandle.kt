package com.klyx.terminal.ui.selection

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.klyx.terminal.ui.selection.icon.TextSelectHandleLeft
import com.klyx.terminal.ui.selection.icon.TextSelectHandleRight

private class HandlePositionProvider(private val position: Offset) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = position.round()
}

internal enum class HandleType {
    Start, End
}

@Composable
private fun SelectionHandle(
    type: HandleType,
    selectionState: SelectionState,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragStart: () -> Unit,
    handleScale: Float = 0.7f,
    brush: Brush = SolidColor(Color(0xFF2196F3))
) {
    val orientation by remember {
        when (type) {
            HandleType.Start -> selectionState.startHandleOrientation
            HandleType.End -> selectionState.endHandleOrientation
        }
    }

    val positionOnScreen = when (type) {
        HandleType.Start -> selectionState.startHandlePosition
        HandleType.End -> selectionState.endHandlePosition
    }

    val handleSize = remember { selectionState.handleSize }

    Popup(
        properties = PopupProperties(
            focusable = false,
            clippingEnabled = false
        ),
        popupPositionProvider = remember(positionOnScreen) {
            HandlePositionProvider(positionOnScreen)
        }
    ) {
        Box(
            modifier = Modifier
                .size(handleSize)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                        onDragStart = { onDragStart() },
                        onDrag = { change, _ ->
                            change.consume()
                            onDrag(change.position)
                        }
                    )
                }
        ) {
            Image(
                imageVector = when (orientation) {
                    HandleOrientation.Left -> TextSelectHandleLeft
                    HandleOrientation.Right -> TextSelectHandleRight
                },
                contentDescription = null,
                modifier = Modifier.size(handleSize),
            )
        }
    }
}

@Composable
internal fun SelectionOverlay(selectionState: SelectionState) {
    if (selectionState.isActive) {
        SelectionHandle(
            type = HandleType.Start,
            selectionState = selectionState,
            onDrag = {},
            onDragEnd = {},
            onDragStart = {},
        )

        SelectionHandle(
            type = HandleType.End,
            selectionState = selectionState,
            onDrag = {},
            onDragEnd = {},
            onDragStart = {},
        )
    }
}
