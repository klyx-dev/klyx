package com.klyx.editor.compose.selection.contextmenu.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import com.klyx.editor.compose.selection.contextmenu.builder.TextContextMenuBuilderScope

internal fun Modifier.addTextContextMenuComponents(
    builder: TextContextMenuBuilderScope.() -> Unit,
): Modifier = this then AddTextContextMenuDataComponentsElement(builder)

private class AddTextContextMenuDataComponentsElement(
    private val builder: TextContextMenuBuilderScope.() -> Unit,
) : ModifierNodeElement<AddTextContextMenuDataComponentsNode>() {
    override fun create(): AddTextContextMenuDataComponentsNode =
        AddTextContextMenuDataComponentsNode(builder)

    override fun update(node: AddTextContextMenuDataComponentsNode) {
        node.builder = builder
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "addTextContextMenuDataComponentsWithLocalization"
        properties["builder"] = builder
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AddTextContextMenuDataComponentsElement) return false

        if (builder !== other.builder) return false

        return true
    }

    override fun hashCode(): Int = builder.hashCode()
}

private class AddTextContextMenuDataComponentsNode(
    var builder: TextContextMenuBuilderScope.() -> Unit,
) : DelegatingNode(), CompositionLocalConsumerModifierNode {
    init {
        delegate(AddTextContextMenuDataComponentsNode { builder() })
    }
}
