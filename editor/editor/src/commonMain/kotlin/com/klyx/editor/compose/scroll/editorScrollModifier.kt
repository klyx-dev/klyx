package com.klyx.editor.compose.scroll

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

@Stable
internal fun Modifier.editorScroll(
    state: EditorScrollState,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
) = editorScroll(
    state = state,
    orientation = orientation,
    enabled = enabled,
    reverseDirection = reverseDirection,
    flingBehavior = flingBehavior,
    interactionSource = interactionSource,
    overscrollEffect = null,
)

@Stable
internal fun Modifier.editorScroll(
    state: EditorScrollState,
    orientation: Orientation,
    overscrollEffect: OverscrollEffect?,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
    bringIntoViewSpec: BringIntoViewSpec? = null,
) = this then
        EditorScrollElement(
            state,
            orientation,
            overscrollEffect,
            enabled,
            reverseDirection,
            flingBehavior,
            interactionSource,
            bringIntoViewSpec,
        )

private class EditorScrollElement(
    val state: EditorScrollState,
    val orientation: Orientation,
    val overscrollEffect: OverscrollEffect?,
    val enabled: Boolean,
    val reverseDirection: Boolean,
    val flingBehavior: FlingBehavior?,
    val interactionSource: MutableInteractionSource?,
    val bringIntoViewSpec: BringIntoViewSpec?,
) : ModifierNodeElement<EditorScrollNode>() {

    override fun create(): EditorScrollNode {
        return EditorScrollNode(
            state,
            overscrollEffect,
            flingBehavior,
            orientation,
            enabled,
            reverseDirection,
            interactionSource,
            bringIntoViewSpec,
        )
    }

    override fun update(node: EditorScrollNode) {
        node.update(
            state,
            orientation,
            overscrollEffect,
            enabled,
            reverseDirection,
            flingBehavior,
            interactionSource,
            bringIntoViewSpec,
        )
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + orientation.hashCode()
        result = 31 * result + overscrollEffect.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + reverseDirection.hashCode()
        result = 31 * result + flingBehavior.hashCode()
        result = 31 * result + interactionSource.hashCode()
        result = 31 * result + bringIntoViewSpec.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is EditorScrollElement) return false

        if (state != other.state) return false
        if (orientation != other.orientation) return false
        if (overscrollEffect != other.overscrollEffect) return false
        if (enabled != other.enabled) return false
        if (reverseDirection != other.reverseDirection) return false
        if (flingBehavior != other.flingBehavior) return false
        if (interactionSource != other.interactionSource) return false
        if (bringIntoViewSpec != other.bringIntoViewSpec) return false

        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "editorScroll"
        properties["orientation"] = orientation
        properties["state"] = state
        properties["overscrollEffect"] = overscrollEffect
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["flingBehavior"] = flingBehavior
        properties["interactionSource"] = interactionSource
        properties["bringIntoViewSpec"] = bringIntoViewSpec
    }
}
