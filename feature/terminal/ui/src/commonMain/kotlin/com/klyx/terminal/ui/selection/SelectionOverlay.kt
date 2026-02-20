package com.klyx.terminal.ui.selection

import androidx.compose.runtime.Composable

@Composable
internal fun SelectionOverlay(
    selectionState: SelectionState,
    containerBounds: ContainerBounds,
    containerPaddingPx: Float = 0f,
    onUpdatePosition: (type: HandleType, screenX: Int, screenY: Int) -> Unit,
) {
    if (!selectionState.isActive) return

    SelectionHandle(
        state = selectionState.startHandle,
        containerBounds = containerBounds,
        containerPaddingPx = containerPaddingPx,
        onUpdatePosition = { x, y -> onUpdatePosition(HandleType.Start, x, y) },
        onDragStarted = { },
        onDragEnded = { },
    )

    SelectionHandle(
        state = selectionState.endHandle,
        containerBounds = containerBounds,
        containerPaddingPx = containerPaddingPx,
        onUpdatePosition = { x, y -> onUpdatePosition(HandleType.End, x, y) },
        onDragStarted = { },
        onDragEnded = { },
    )
}
