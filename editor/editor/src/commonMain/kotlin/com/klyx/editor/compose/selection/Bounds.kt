package com.klyx.editor.compose.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow

/** Returns the boundary of the visible area in this [LayoutCoordinates]. */
internal fun LayoutCoordinates.visibleBounds(): Rect {
    // globalBounds is the global boundaries of this LayoutCoordinates after it's clipped by
    // parents. We can think it as the global visible bounds of this Layout. Here globalBounds
    // is convert to local, which is the boundary of the visible area within the LayoutCoordinates.
    val boundsInWindow = boundsInWindow()
    return Rect(windowToLocal(boundsInWindow.topLeft), windowToLocal(boundsInWindow.bottomRight))
}

internal fun Rect.containsInclusive(offset: Offset): Boolean =
    offset.x in left..right && offset.y in top..bottom
