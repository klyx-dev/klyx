package com.klyx.terminal.ui.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import com.klyx.terminal.emulator.TerminalBuffer
import com.klyx.terminal.emulator.WcWidth
import com.klyx.terminal.ui.FontMetrics
import com.klyx.terminal.ui.TerminalState
import com.klyx.util.clipboard.clipEntryOf
import com.klyx.util.clipboard.paste
import com.klyx.util.toCodePoint
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun rememberSelectionController(state: TerminalState): SelectionController {
    val clipboard = LocalClipboard.current
    return remember(clipboard, state) {
        SelectionController(state, clipboard)
    }
}

@Stable
internal class SelectionController(
    private val state: TerminalState,
    private val clipboard: Clipboard
) {
    var selX1 by mutableIntStateOf(-1)
        private set
    var selY1 by mutableIntStateOf(-1)
        private set
    var selX2 by mutableIntStateOf(-1)
        private set
    var selY2 by mutableIntStateOf(-1)
        private set

    var isActive by mutableStateOf(false)
        private set

    var startHandlePosition by mutableStateOf(Offset.Zero)
        private set
    var endHandlePosition by mutableStateOf(Offset.Zero)
        private set

    var startHandleOrientation by mutableStateOf(HandleOrientation.Left)
        private set
    var endHandleOrientation by mutableStateOf(HandleOrientation.Right)
        private set

    var isStartHandleDragging by mutableStateOf(false)
        private set
    var isEndHandleDragging by mutableStateOf(false)
        private set

    private var showStartTime = Clock.System.now()
    private var storedSelectedText: String? = null

    private val emulator get() = checkNotNull(state.emulator) {
        "Terminal emulator should not be null at this point."
    }

    fun getSelectedText(): String {
        val emulator = state.emulator ?: return ""
        return emulator.getSelectedText(selX1, selY1, selX2, selY2)
    }

    suspend fun copySelectedText() {
        val text = getSelectedText()
        if (text.isNotEmpty()) {
            clipboard.setClipEntry(clipEntryOf(text))
        }
    }

    suspend fun pasteText() {
        val text = clipboard.paste()
        if (!text.isNullOrEmpty()) {
            state.emulator?.paste(text)
        }
    }

    fun show(offset: Offset, metrics: FontMetrics) {
        val col = (offset.x / metrics.width).toInt()
        val row = ((offset.y - metrics.ascent) / metrics.height + state.topRow.intValue).toInt()

        setInitialTextSelectionPosition(col, row)

        updateHandlePositions(metrics)

        showStartTime = Clock.System.now()
        isActive = true

        state.isSelectingText.value = true
        state.selectionX1.intValue = selX1
        state.selectionY1.intValue = selY1
        state.selectionX2.intValue = selX2
        state.selectionY2.intValue = selY2
    }

    fun hide(): Boolean {
        if (!isActive) return false

        if (Clock.System.now() - showStartTime < 300.milliseconds) {
            return false
        }

        selX1 = -1
        selY1 = -1
        selX2 = -1
        selY2 = -1
        isActive = false

        state.stopTextSelection()

        return true
    }

    fun render(metrics: FontMetrics) {
        if (!isActive) return

        updateHandlePositions(metrics)

        // Update state
        state.selectionX1.intValue = selX1
        state.selectionY1.intValue = selY1
        state.selectionX2.intValue = selX2
        state.selectionY2.intValue = selY2
    }

    private fun setInitialTextSelectionPosition(col: Int, row: Int) {
        selX1 = col
        selX2 = col
        selY1 = row
        selY2 = row

        val emulator = state.emulator ?: return
        val screen = emulator.screen

        // Check if we're selecting something other than whitespace
        val selectedChar = screen.getSelectedText(selX1, selY1, selX1, selY1)
        if (selectedChar != " " && selectedChar.isNotEmpty()) {
            // Expand to word
            while (selX1 > 0) {
                val prevChar = screen.getSelectedText(selX1 - 1, selY1, selX1 - 1, selY1)
                if (prevChar.isEmpty() || prevChar == " ") break
                selX1--
            }

            while (selX2 < emulator.columns - 1) {
                val nextChar = screen.getSelectedText(selX2 + 1, selY1, selX2 + 1, selY1)
                if (nextChar.isEmpty() || nextChar == " ") break
                selX2++
            }
        }
    }

    private fun updateHandlePositions(metrics: FontMetrics) {
        val x1 = state.getPointX(selX1)
        val y1 = state.getPointY(selY1 + 1) + metrics.ascent
        val x2 = state.getPointX(selX2 + 1)
        val y2 = state.getPointY(selY2 + 1) + metrics.ascent

        startHandlePosition = Offset(
            x = x1 - (if (startHandleOrientation == HandleOrientation.Left) 18f else 6f),
            y = y1
        )

        endHandlePosition = Offset(
            x = x2 - (if (endHandleOrientation == HandleOrientation.Right) 18f else 6f),
            y = y2
        )
    }

    fun onStartHandleDragStart() {
        isStartHandleDragging = true
    }

    fun onStartHandleDrag(dragAmount: Offset, metrics: FontMetrics) {
        if (!isActive) return

        val newX = startHandlePosition.x + dragAmount.x.roundToInt()
        val newY = startHandlePosition.y + dragAmount.y.roundToInt()

        updatePosition(
            isStartHandle = true,
            x = newX,
            y = newY,
            metrics = metrics
        )
    }

    fun onStartHandleDragEnd() {
        isStartHandleDragging = false
    }

    fun onEndHandleDragStart() {
        isEndHandleDragging = true
    }

    fun onEndHandleDrag(dragAmount: Offset, metrics: FontMetrics) {
        if (!isActive) return

        val newX = endHandlePosition.x + dragAmount.x.roundToInt()
        val newY = endHandlePosition.y + dragAmount.y.roundToInt()

        updatePosition(
            isStartHandle = false,
            x = newX,
            y = newY,
            metrics = metrics
        )
    }

    fun onEndHandleDragEnd() {
        isEndHandleDragging = false
    }

    private fun updatePosition(
        isStartHandle: Boolean,
        x: Float,
        y: Float,
        metrics: FontMetrics
    ) {
        val emulator = state.emulator ?: return
        val screen = emulator.screen
        val scrollRows = screen.activeTranscriptRows

        val cursorX = (x / metrics.width).toInt()
        val cursorY = ((y - metrics.ascent) / metrics.height).roundToInt() + state.topRow.intValue

        if (isStartHandle) {
            selX1 = cursorX.coerceIn(0, emulator.columns - 1)
            selY1 = cursorY.coerceIn(-scrollRows, emulator.rows - 1)

            // Ensure start is before or equal to end
            if (selY1 > selY2) {
                selY1 = selY2
            }
            if (selY1 == selY2 && selX1 > selX2) {
                selX1 = selX2
            }

            // Auto-scroll if needed
            if (!emulator.isAlternateBufferActive) {
                var topRow = state.topRow.intValue

                if (selY1 <= topRow) {
                    topRow = (topRow - 1).coerceAtLeast(-scrollRows)
                } else if (selY1 >= topRow + emulator.rows) {
                    topRow = (topRow + 1).coerceAtMost(0)
                }

                state.topRow.intValue = topRow
            }

            selX1 = getValidCursorX(screen, selY1, selX1)

        } else {
            selX2 = cursorX.coerceIn(0, emulator.columns - 1)
            selY2 = cursorY.coerceIn(-scrollRows, emulator.rows - 1)

            // Ensure end is after or equal to start
            if (selY1 > selY2) {
                selY2 = selY1
            }
            if (selY1 == selY2 && selX1 > selX2) {
                selX2 = selX1
            }

            // Auto-scroll if needed
            if (!emulator.isAlternateBufferActive) {
                var topRow = state.topRow.intValue

                if (selY2 <= topRow) {
                    topRow = (topRow - 1).coerceAtLeast(-scrollRows)
                } else if (selY2 >= topRow + emulator.rows) {
                    topRow = (topRow + 1).coerceAtMost(0)
                }

                state.topRow.intValue = topRow
            }

            selX2 = getValidCursorX(screen, selY2, selX2)
        }

        // Update state
        state.selectionX1.intValue = selX1
        state.selectionY1.intValue = selY1
        state.selectionX2.intValue = selX2
        state.selectionY2.intValue = selY2

        updateHandlePositions(metrics)
        state.invalidate()
    }

    private fun getValidCursorX(screen: TerminalBuffer, cy: Int, cx: Int): Int {
        val line = screen.getSelectedText(0, cy, cx, cy)
        if (line.isEmpty()) return cx

        var col = 0
        var i = 0
        val len = line.length

        while (i < len) {
            val ch1 = line[i]
            if (ch1.code == 0) break

            val wc = if (ch1.isHighSurrogate() && i + 1 < len) {
                val ch2 = line[++i]
                WcWidth.width(Char.toCodePoint(ch1, ch2))
            } else {
                WcWidth.width(ch1.code)
            }

            val cend = col + wc
            if (cx in (col + 1)..<cend) return cend
            if (cend == col) return col
            col = cend
            i++
        }

        return cx
    }

    fun decrementYTextSelectionCursors(decrement: Int) {
        selY1 -= decrement
        selY2 -= decrement
        state.selectionY1.intValue = selY1
        state.selectionY2.intValue = selY2
    }

    fun getStoredSelectedText(): String? = storedSelectedText

    fun storeSelectedText() {
        storedSelectedText = getSelectedText()
    }

    fun clearStoredSelectedText() {
        storedSelectedText = null
    }
}
