package com.klyx.terminal.ui.selection

internal data class ContainerBounds(
    /** Screen-pixel X of the container's left edge. */
    val left: Float,
    /** Screen-pixel Y of the container's top edge. */
    val top: Float,
    /** Screen-pixel X of the container's right edge. */
    val right: Float,
    /** Screen-pixel Y of the container's bottom edge. */
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    companion object {
        val Zero = ContainerBounds(0f, 0f, 0f, 0f)
    }
}
