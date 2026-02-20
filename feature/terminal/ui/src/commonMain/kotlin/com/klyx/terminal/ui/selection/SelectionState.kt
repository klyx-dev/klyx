package com.klyx.terminal.ui.selection

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Top-level selection state that owns both handles.
 *
 * The terminal renderer calls [startSelection], [updateStartHandle],
 * [updateEndHandle], and [clearSelection] in response to touch events
 * on the terminal surface.
 *
 * @param handleWidthPx   Intrinsic pixel width  of the handle drawables.
 * @param handleHeightPx  Intrinsic pixel height of the handle drawables.
 */
@Stable
internal class SelectionState(
    handleWidthPx: Float,
    handleHeightPx: Float,
) {
    // ── Handles ───────────────────────────────────────────────────────────────

    /**
     * The start (left) handle — initial orientation [HandleOrientation.Left].
     * Mirrors the handle created with LEFT in TextSelectionHandleView.
     */
    val startHandle = HandleState(
        initialOrientation = HandleOrientation.Left,
        handleWidthPx      = handleWidthPx,
        handleHeightPx     = handleHeightPx,
    )

    /**
     * The end (right) handle — initial orientation [HandleOrientation.Right].
     * Mirrors the handle created with RIGHT in TextSelectionHandleView.
     */
    val endHandle = HandleState(
        initialOrientation = HandleOrientation.Right,
        handleWidthPx      = handleWidthPx,
        handleHeightPx     = handleHeightPx,
    )

    // ── Selection activity ────────────────────────────────────────────────────

    /** True when a selection is active and the overlay should be shown. */
    var isActive by mutableStateOf(false)
        private set

    // ── Terminal cell selection bounds ────────────────────────────────────────
    // These are in terminal column / row coordinates, not pixels.

    /** Column of the selection start. */
    var selectionStartColumn by mutableStateOf(0)
        private set

    /** Row of the selection start. */
    var selectionStartRow by mutableStateOf(0)
        private set

    /** Column of the selection end. */
    var selectionEndColumn by mutableStateOf(0)
        private set

    /** Row of the selection end. */
    var selectionEndRow by mutableStateOf(0)
        private set

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Begin a new selection.  Both handles are hidden until
     * [updateStartHandle] / [updateEndHandle] are called.
     */
    fun startSelection(
        startCol: Int, startRow: Int,
        endCol:   Int, endRow:   Int,
    ) {
        selectionStartColumn = startCol
        selectionStartRow    = startRow
        selectionEndColumn   = endCol
        selectionEndRow      = endRow
        isActive             = true
    }

    /**
     * Move the start handle to the given screen-pixel position.
     * Mirrors positionAtCursor() called on the LEFT handle.
     *
     * @param screenX  Screen-pixel X (typically terminalView.getPointX(col)).
     * @param screenY  Screen-pixel Y (typically terminalView.getPointY(row + 1)).
     * @param col      Terminal column — stored so callers can query the cell.
     * @param row      Terminal row.
     */
    fun updateStartHandle(screenX: Float, screenY: Float, col: Int = selectionStartColumn, row: Int = selectionStartRow) {
        selectionStartColumn = col
        selectionStartRow    = row
        startHandle.moveTo(screenX, screenY)
        startHandle.isVisible = true
    }

    /**
     * Move the end handle to the given screen-pixel position.
     * Mirrors positionAtCursor() called on the RIGHT handle.
     */
    fun updateEndHandle(screenX: Float, screenY: Float, col: Int = selectionEndColumn, row: Int = selectionEndRow) {
        selectionEndColumn = col
        selectionEndRow    = row
        endHandle.moveTo(screenX, screenY)
        endHandle.isVisible = true
    }

    /**
     * Hide both handles and deactivate the selection.
     * Mirrors hide() on both handles + clearing the selection.
     */
    fun clearSelection() {
        startHandle.isVisible  = false
        startHandle.isDragging = false
        endHandle.isVisible    = false
        endHandle.isDragging   = false
        isActive               = false
    }

    /**
     * Hide just the start handle (e.g. it scrolled off screen).
     * Mirrors hide() being called from moveTo() when !isPositionVisible().
     */
    fun hideStartHandle() {
        startHandle.isVisible  = false
        startHandle.isDragging = false
    }

    /**
     * Hide just the end handle.
     */
    fun hideEndHandle() {
        endHandle.isVisible  = false
        endHandle.isDragging = false
    }

    // ── Convenience getters ────────────────────────────────────────────────────

    /** True if either handle is currently being dragged. */
    val isDragging: Boolean
        get() = startHandle.isDragging || endHandle.isDragging

    /** Returns the [HandleState] for the given [HandleType]. */
    fun handleFor(type: HandleType): HandleState = when (type) {
        HandleType.Start -> startHandle
        HandleType.End   -> endHandle
    }
}
