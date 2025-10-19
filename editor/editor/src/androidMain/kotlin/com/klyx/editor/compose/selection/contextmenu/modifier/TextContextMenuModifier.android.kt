package com.klyx.editor.compose.selection.contextmenu.modifier

import android.content.Context
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import com.klyx.editor.compose.selection.contextmenu.builder.TextContextMenuBuilderScope

internal fun Modifier.addTextContextMenuComponentsWithContext(
    builder: TextContextMenuBuilderScope.(Context) -> Unit
): Modifier = this then AddTextContextMenuDataComponentsWithContextElement(builder)

private class AddTextContextMenuDataComponentsWithContextElement(
    private val builder: TextContextMenuBuilderScope.(Context) -> Unit
) : ModifierNodeElement<AddTextContextMenuDataComponentsWithContextNode>() {
    override fun create(): AddTextContextMenuDataComponentsWithContextNode =
        AddTextContextMenuDataComponentsWithContextNode(builder)

    override fun update(node: AddTextContextMenuDataComponentsWithContextNode) {
        node.builder = builder
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "addTextContextMenuDataComponentsWithResources"
        properties["builder"] = builder
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AddTextContextMenuDataComponentsWithContextElement) return false

        if (builder !== other.builder) return false

        return true
    }

    override fun hashCode(): Int = builder.hashCode()
}

private class AddTextContextMenuDataComponentsWithContextNode(
    var builder: TextContextMenuBuilderScope.(Context) -> Unit
) : DelegatingNode(), CompositionLocalConsumerModifierNode {
    init {
        delegate(AddTextContextMenuDataComponentsNode { builder(currentValueOf(LocalContext)) })
    }
}
