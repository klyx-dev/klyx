package com.klyx.terminal.ui.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.klyx.terminal.ui.TerminalState

@Composable
internal fun rememberSelectionState(
    terminalState: TerminalState,
    handleSize: DpSize = DpSize(24.dp, 24.dp)
): SelectionState {
    val density = LocalDensity.current
    return remember(terminalState, density) { SelectionState(terminalState, handleSize, density) }
}

@Stable
internal class SelectionState(
    private val terminalState: TerminalState,
    val handleSize: DpSize,
    private val density: Density
) {
    var selX1 by terminalState.selectionX1
        private set
    var selY1 by terminalState.selectionY1
        private set
    var selX2 by terminalState.selectionX2
        private set
    var selY2 by terminalState.selectionY2
        private set

    var isActive by terminalState.isSelectingText
        private set

    var isDragging by mutableStateOf(false)

    private val emulator
        get() = checkNotNull(terminalState.emulator) {
            "Terminal emulator should not be null at this point."
        }

    var startHandleOrientation = mutableStateOf(HandleOrientation.Left)
    var endHandleOrientation = mutableStateOf(HandleOrientation.Right)

    var startHandlePosition by mutableStateOf(Offset.Zero)
    var endHandlePosition by mutableStateOf(Offset.Zero)

    val handleHeight = with(density) { handleSize.height.toPx() }
    val handleWidth = with(density) { handleSize.width.toPx() }

    var startPointX = 0
    var startPointY = 0
    var endPointX = 0
    var endPointY = 0

    var startHotspotX by mutableFloatStateOf(0f)
    var startHotspotY by mutableFloatStateOf(0f)
    var endHotspotX by mutableFloatStateOf(0f)
    var endHotspotY by mutableFloatStateOf(0f)

    var startTouchOffsetY = 0f
    var endTouchOffsetY = 0f

    init {
        startHotspotX = (handleWidth * 3f) / 4f
        endHotspotX = handleWidth / 4f

        startTouchOffsetY = -handleHeight * 0.3f
        endTouchOffsetY = -handleHeight * 0.3f

        startHotspotY = 0f
        endHotspotY = 0f
    }

    fun showAt(offset: Offset) {
        setInitialTextSelectionPosition(offset)
    }

    private fun setInitialTextSelectionPosition(offset: Offset) {
        val cell = terminalState.cellAt(offset)
        selX1 = cell.column
        selY1 = cell.row
        selX2 = cell.column
        selY2 = cell.row

        val screen = emulator.screen
        if (screen.getSelectedText(selX1, selY1, selX2, selY2) != " ") {
            // Selecting something other than whitespace. Expand to word.
            while (selX1 > 0 && screen.getSelectedText(selX1 - 1, selY1, selX1 - 1, selY1).isNotEmpty()) {
                selX1--
            }
            while (
                selX2 < emulator.columns - 1 && screen.getSelectedText(selX2 + 1, selY1, selX2 + 1, selY1).isNotEmpty()
            ) {
                selX2++
            }
        }
    }

    private fun updateHandlePositions(type: HandleType) {
        when (type) {
            HandleType.Start -> {
                val x = terminalState.getPointX(selX1)
                val y = terminalState.getPointY(selY1 + 1)
            }

            HandleType.End -> {
                val x = terminalState.getPointX(selX2 + 1)
                val y = terminalState.getPointY(selY2 + 1)
            }
        }
    }
}
