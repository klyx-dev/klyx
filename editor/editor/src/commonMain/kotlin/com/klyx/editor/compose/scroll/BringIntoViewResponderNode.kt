@file:Suppress("DEPRECATION")

package com.klyx.editor.compose.scroll

import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.relocation.bringIntoView
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * A modifier that holds state and modifier implementations for [androidx.compose.foundation.relocation.bringIntoViewResponder]. It has
 * parent access to the next [BringIntoViewModifierNode] via
 * [androidx.compose.ui.relocation.bringIntoView] and additionally provides itself as the
 * [BringIntoViewModifierNode] for subsequent modifiers. This class is responsible for recursively
 * propagating requests up the responder chain.
 */
internal class BringIntoViewResponderNode(
    var responder: BringIntoViewResponder
) : Modifier.Node(), BringIntoViewModifierNode, LayoutAwareModifierNode {

    override val shouldAutoInvalidate: Boolean = false

    // TODO(b/324613946) Get rid of this check.
    private var hasBeenPlaced = false

    override fun onPlaced(coordinates: LayoutCoordinates) {
        hasBeenPlaced = true
    }

    /**
     * Responds to a child's request by first converting [boundsProvider] into this node's
     * [LayoutCoordinates] and then, concurrently, calling the [responder] and the [parent] to
     * handle the request.
     */
    override suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?,
    ) {
        @Suppress("NAME_SHADOWING")
        fun localRect(): Rect? {
            if (!isAttached) return null
            // Can't do any calculations before the node is initially placed.
            if (!hasBeenPlaced) return null

            // Either coordinates can become detached at any time, so we have to check before every
            // calculation.
            val layoutCoordinates = requireLayoutCoordinates()
            val childCoordinates = childCoordinates.takeIf { it.isAttached } ?: return null
            val rect = boundsProvider() ?: return null
            return layoutCoordinates.localRectOf(childCoordinates, rect)
        }

        val parentRect = { localRect()?.let(responder::calculateRectForParent) }

        coroutineScope {
            // For the item to be visible, if needs to be in the viewport of all its
            // ancestors.
            // Note: For now we run both of these concurrently, but in the future we could
            // make this configurable. (The child relocation could be executed before the
            // parent, or parent before the child).
            launch {
                // Bring the requested Child into this parent's view.
                responder.bringChildIntoView(::localRect)
            }

            // Launch this as well so that if the parent is cancelled (this throws a CE) due to
            // animation interruption, the child continues animating. If we just call
            // bringChildIntoView directly without launching, if that function throws a
            // CancellationException, it will cancel this coroutineScope, which will also cancel the
            // responder's coroutine.
            launch { bringIntoView(parentRect) }
        }
    }
}

/** Translates [rect], specified in [sourceCoordinates], into this [LayoutCoordinates]. */
private fun LayoutCoordinates.localRectOf(sourceCoordinates: LayoutCoordinates, rect: Rect): Rect {
    // Translate the supplied layout coordinates into the coordinate system of this parent.
    val localRect = localBoundingBoxOf(sourceCoordinates, clipBounds = false)

    // Translate the rect to this parent's local coordinates.
    return rect.translate(localRect.topLeft)
}
