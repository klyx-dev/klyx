package com.klyx.terminal.ui.selection

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

@Stable
class TextSelectionState {
    var isActive by mutableStateOf(false)
        private set

    var selX1 by mutableStateOf(-1); private set
    var selY1 by mutableStateOf(-1); private set
    var selX2 by mutableStateOf(-1); private set
    var selY2 by mutableStateOf(-1); private set

    /**
     * Stored text captured just before "More" was pressed, so the caller
     * can still read it after selection is cleared.
     */
    var storedSelectedText: String? by mutableStateOf(null)
        private set

    private var showStartTime = TimeSource.Monotonic.markNow()

    fun show(x: Int, y: Int) {
        selX1 = x; selX2 = x
        selY1 = y; selY2 = y
        isActive = true
        showStartTime = TimeSource.Monotonic.markNow()
    }

    fun hide(): Boolean {
        if (!isActive) return false
        if (showStartTime.elapsedNow() < 300.milliseconds) return false
        selX1 = -1; selY1 = -1; selX2 = -1; selY2 = -1
        isActive = false
        return true
    }

    fun initWordSelection(
        x: Int,
        y: Int,
        maxColumns: Int,
        isBlank: (col: Int, row: Int) -> Boolean,
    ) {
        selX1 = x; selX2 = x; selY1 = y; selY2 = y
        if (isBlank(x, y)) return

        var left = x
        while (left > 0 && !isBlank(left - 1, y)) left--

        var right = x
        while (right < maxColumns - 1 && !isBlank(right + 1, y)) right++

        selX1 = left
        selX2 = right
    }

    fun updateStartHandle(
        newX: Int,
        newY: Int,
        maxRows: Int,
        scrollRows: Int,
    ) {
        var x = newX.coerceAtLeast(0)
        var y = newY.coerceIn(-scrollRows, maxRows - 1)
        // start must not exceed end
        if (y > selY2) y = selY2
        if (y == selY2 && x > selX2) x = selX2
        selX1 = x; selY1 = y
    }

    fun updateEndHandle(
        newX: Int,
        newY: Int,
        maxRows: Int,
        scrollRows: Int,
    ) {
        var x = newX.coerceAtLeast(0)
        var y = newY.coerceIn(-scrollRows, maxRows - 1)
        // end must not precede start
        if (selY1 > y) y = selY1
        if (selY1 == y && selX1 > x) x = selX1
        selX2 = x; selY2 = y
    }

    fun decrementYCursors(amount: Int) {
        selY1 -= amount
        selY2 -= amount
    }

    fun getSelectors(): IntArray = intArrayOf(selY1, selY2, selX1, selX2)

    fun storeSelectedText(text: String) {
        storedSelectedText = text
    }

    fun clearStoredSelectedText() {
        storedSelectedText = null
    }
}
