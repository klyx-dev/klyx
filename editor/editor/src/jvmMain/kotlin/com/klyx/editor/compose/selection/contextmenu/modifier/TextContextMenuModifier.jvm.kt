package com.klyx.editor.compose.selection.contextmenu.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalLocalization
import androidx.compose.ui.platform.PlatformLocalization
import com.klyx.editor.compose.selection.contextmenu.builder.TextContextMenuBuilderScope

internal fun Modifier.addTextContextMenuComponentsWithLocalization(
    builder: TextContextMenuBuilderScope.(PlatformLocalization) -> Unit,
): Modifier = this then AddTextContextMenuDataComponentsWithLocalizationElement(builder)

private class AddTextContextMenuDataComponentsWithLocalizationElement(
    private val builder: TextContextMenuBuilderScope.(PlatformLocalization) -> Unit,
) : ModifierNodeElement<AddTextContextMenuDataComponentsWithLocalizationNode>() {
    override fun create(): AddTextContextMenuDataComponentsWithLocalizationNode =
        AddTextContextMenuDataComponentsWithLocalizationNode(builder)

    override fun update(node: AddTextContextMenuDataComponentsWithLocalizationNode) {
        node.builder = builder
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "addTextContextMenuDataComponentsWithLocalization"
        properties["builder"] = builder
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AddTextContextMenuDataComponentsWithLocalizationElement) return false

        if (builder !== other.builder) return false

        return true
    }

    override fun hashCode(): Int = builder.hashCode()
}

private class AddTextContextMenuDataComponentsWithLocalizationNode(
    var builder: TextContextMenuBuilderScope.(PlatformLocalization) -> Unit,
) : DelegatingNode(), CompositionLocalConsumerModifierNode {
    init {
        delegate(AddTextContextMenuDataComponentsNode { builder(currentValueOf(LocalLocalization)) })
    }
}
