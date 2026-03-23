package com.klyx.nodegraph.extension

import androidx.compose.ui.geometry.Offset
import com.klyx.nodegraph.NodeData
import com.klyx.nodegraph.NodeKind
import com.klyx.nodegraph.NodePin
import com.klyx.nodegraph.NodeRegistry
import com.klyx.nodegraph.PinDirection
import com.klyx.nodegraph.PinType
import com.klyx.nodegraph.getVariableKey
import com.klyx.nodegraph.setVariableKey
import com.klyx.nodegraph.util.generateId
import kotlin.uuid.Uuid

data class Variable(
    val name: String,
    val type: PinType,
    val defaultValue: Any? = null,
    val isSystem: Boolean = false,
    val id: Uuid = generateId()
)

fun Variable.createGetNode(position: Offset): NodeData {
    val nodeId = generateId()
    val node = NodeData(
        id = nodeId,
        title = "Get $name",
        definitionKey = getVariableKey(id),
        kind = NodeKind.Custom,
        headerColor = type.color,
        pins = listOf(NodePin(generateId(), name, type, PinDirection.Output, nodeId)),
        position = position,
    )
    return node
}

fun Variable.createSetNode(position: Offset): NodeData {
    val nodeId = generateId()
    val node = NodeData(
        id = nodeId,
        title = "Set $name",
        definitionKey = setVariableKey(id),
        kind = NodeKind.Custom,
        headerColor = type.color,
        pins = listOf(
            NodePin(generateId(), "Exec", PinType.Flow, PinDirection.Input, nodeId, true),
            NodePin(generateId(), name, type, PinDirection.Input, nodeId),
            NodePin(generateId(), "Then", PinType.Flow, PinDirection.Output, nodeId, true),
            NodePin(generateId(), name, type, PinDirection.Output, nodeId),
        ),
        position = position,
    )
    return node
}

interface GraphExtension {
    val name: String
    val version: String get() = "1.0.0"

    /** Variables that must be injected into the graph when this extension is used. */
    val variables: List<Variable> get() = emptyList()

    /** Called once when installed. register your nodes here. */
    fun install(registry: NodeRegistry)

    /** Called when uninstalled. default is no-op. */
    fun uninstall(registry: NodeRegistry) {}
}
