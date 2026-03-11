package com.klyx.terminal.ui.selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun TextSelectionOverlay(
    state: TextSelectionState,
    fontWidth: Float,
    fontLineSpacing: Float,
    topRow: Int,
    maxColumns: Int,
    maxRows: Int,
    scrollRows: Int,
    leftHandleIcon: ImageVector,
    rightHandleIcon: ImageVector,
    modifier: Modifier = Modifier,
    handleTint: Color = Color.Unspecified,
    canPaste: Boolean = false,
    getSelectedText: () -> String = { "" },
    onCopy: (String) -> Unit = {},
    onPaste: () -> Unit = {},
    onMore: () -> Unit = {},
    onScrollRequest: (rows: Int) -> Unit = {},
) {
    if (!state.isActive) return

    // start handle sits one row below the top-left of the selection.
    val startPixelX by remember(state.selX1, fontWidth) {
        derivedStateOf { state.selX1 * fontWidth }
    }
    val startPixelY by remember(state.selY1, fontLineSpacing, topRow) {
        derivedStateOf { (state.selY1 + 1 - topRow) * fontLineSpacing }
    }

    // end handle sits one column to the right and one row below the bottom-right.
    val endPixelX by remember(state.selX2, fontWidth) {
        derivedStateOf { (state.selX2 + 1) * fontWidth }
    }
    val endPixelY by remember(state.selY2, fontLineSpacing, topRow) {
        derivedStateOf { (state.selY2 + 1 - topRow) * fontLineSpacing }
    }

    // toolbar anchors at the horizontal centre, above the higher handle.
    val toolbarAnchorX by remember(startPixelX, endPixelX) {
        derivedStateOf { (startPixelX + endPixelX) / 2f }
    }
    val toolbarAnchorY by remember(startPixelY, endPixelY) {
        derivedStateOf { minOf(startPixelY, endPixelY) }
    }

    fun pixelXToCol(px: Float): Int =
        (px / fontWidth).toInt().coerceIn(0, maxColumns - 1)

    fun pixelYToRow(py: Float): Int = (((py - 40f) / fontLineSpacing) + topRow).toInt()

    fun maybeScroll(row: Int) {
        when {
            row <= topRow -> onScrollRequest(-1)
            row >= topRow + maxRows -> onScrollRequest(+1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SelectionHandle(
            orientation = HandleOrientation.LEFT,
            anchorX = startPixelX,
            anchorY = startPixelY,
            leftIcon = leftHandleIcon,
            rightIcon = rightHandleIcon,
            tint = handleTint,
            onDragStart = {},
            onDragPosition = { px, py ->
                val col = pixelXToCol(px)
                val row = pixelYToRow(py)
                state.updateStartHandle(col, row, maxRows, scrollRows)
                maybeScroll(row)
            },
            onDragEnd = {},
        )

        SelectionHandle(
            orientation = HandleOrientation.RIGHT,
            anchorX = endPixelX,
            anchorY = endPixelY,
            leftIcon = leftHandleIcon,
            rightIcon = rightHandleIcon,
            tint = handleTint,
            onDragStart = {},
            onDragPosition = { px, py ->
                val col = pixelXToCol(px)
                val row = pixelYToRow(py)
                state.updateEndHandle(col, row, maxRows, scrollRows)
                maybeScroll(row)
            },
            onDragEnd = {},
        )

        SelectionActionToolbar(
            visible = state.isActive,
            anchorX = toolbarAnchorX,
            anchorY = toolbarAnchorY,
            canPaste = canPaste,
            onCopy = {
                onCopy(getSelectedText())
                state.hide()
            },
            onPaste = {
                state.hide()
                onPaste()
            },
            onMore = {
                // store text before hiding so the caller can still read it.
                state.storeSelectedText(getSelectedText())
                state.hide()
                onMore()
            },
        )
    }
}
