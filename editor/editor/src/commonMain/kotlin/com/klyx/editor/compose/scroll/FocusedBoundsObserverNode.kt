package com.klyx.editor.compose.scroll

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.findNearestAncestor

internal class FocusedBoundsObserverNode(var onPositioned: (LayoutCoordinates?) -> Unit) :
    Modifier.Node(), TraversableNode {

    override val traverseKey: Any = TraverseKey

    /** Called when a child gains/loses focus or is focused and changes position. */
    fun onFocusBoundsChanged(focusedBounds: LayoutCoordinates?) {
        onPositioned(focusedBounds)
        findNearestAncestor()?.onFocusBoundsChanged(focusedBounds)
    }

    companion object TraverseKey
}
