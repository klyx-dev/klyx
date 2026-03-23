@file:OptIn(ExperimentalSerializationApi::class)

package com.klyx.nodegraph

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.klyx.nodegraph.util.generateId
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.uuid.Uuid

@Immutable
@Serializable
internal data class GraphVariable(
    @ProtoNumber(1) val id: Uuid,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val type: PinType,
    @ProtoNumber(4) val defaultValue: String = "",
    @ProtoNumber(5) val isSystem: Boolean = false
)

internal const val VAR_GET_PREFIX = "var.get:"
internal const val VAR_SET_PREFIX = "var.set:"

internal fun getVariableKey(id: Uuid) = "$VAR_GET_PREFIX$id"
internal fun setVariableKey(id: Uuid) = "$VAR_SET_PREFIX$id"

internal fun isGetVariableKey(key: String) = key.startsWith(VAR_GET_PREFIX)
internal fun isSetVariableKey(key: String) = key.startsWith(VAR_SET_PREFIX)

internal fun variableIdFromKey(key: String): Uuid? = runCatching {
    Uuid.parse(key.removePrefix(VAR_GET_PREFIX).removePrefix(VAR_SET_PREFIX))
}.getOrNull()

internal fun GraphState.addVariable(
    id: Uuid,
    name: String,
    type: PinType,
    defaultValue: Any? = null,
    isSystem: Boolean = false,
    recordUndo: Boolean = true
): GraphVariable {
    val existing = variables.find { it.name == name && it.isSystem }
    if (existing != null) return existing

    val v = GraphVariable(id, name, type, defaultValue as? String ?: "", isSystem)
    variables += v
    if (defaultValue != null && defaultValue !is String && variableValues[v.id] == null) {
        variableValues[v.id] = defaultValue
    } else {
        variableValues[v.id] = parseVariableDefault(defaultValue as? String ?: "", type)
    }
    if (recordUndo) {
        undoStack.push(AddVariableCmd(v, defaultValue))
    }
    markDirty()
    return v
}

/**
 * Removes a variable and all Get/Set nodes that reference it.
 */
internal fun GraphState.removeVariable(variableId: Uuid) {
    val v = variables.find { it.id == variableId } ?: return
    variables.remove(v)
    variableValues.remove(variableId)
    val getKey = getVariableKey(variableId)
    val setKey = setVariableKey(variableId)
    nodes
        .filter { it.definitionKey == getKey || it.definitionKey == setKey }
        .map { it.id }
        .forEach { removeNode(it) }
    markDirty()
    undoStack.push(RemoveVariableCmd(v))
}

/**
 * Renames a variable. Updates all Get/Set node titles automatically.
 */
internal fun GraphState.renameVariable(variableId: Uuid, newName: String) {
    val i = variables.indexOfFirst { it.id == variableId }
    if (i < 0) return
    val oldName = variables[i].name
    variables[i] = variables[i].copy(name = newName)
    val getKey = getVariableKey(variableId)
    val setKey = setVariableKey(variableId)
    nodes.indices.forEach { j ->
        val node = nodes[j]
        if (node.definitionKey == getKey) {
            nodes[j] = node.copy(
                title = "Get $newName",
                pins = node.pins.map { pin -> if (pin.label == oldName) pin.copy(label = newName) else pin }
            )
        } else if (node.definitionKey == setKey) {
            nodes[j] = node.copy(
                title = "Set $newName",
                pins = node.pins.map { pin -> if (pin.label == oldName) pin.copy(label = newName) else pin }
            )
        }
    }
}

/**
 * Creates a Get node for [variable] at [position].
 * The node outputs the variable's current live value at execution time.
 */
internal fun GraphState.createGetNode(variable: GraphVariable, position: Offset): NodeData {
    val nodeId = generateId()
    val node = NodeData(
        id = nodeId,
        title = "Get ${variable.name}",
        definitionKey = getVariableKey(variable.id),
        kind = NodeKind.Custom,
        headerColor = variable.type.color,
        pins = listOf(
            NodePin(generateId(), variable.name, variable.type, PinDirection.Output, nodeId),
        ),
        position = position,
    )
    addNode(node)
    return node
}

/**
 * Creates a Set node for [variable] at [position].
 * The node writes a new value to the variable at execution time and passes
 * the new value through as an output so it can be chained.
 */
internal fun GraphState.createSetNode(variable: GraphVariable, position: Offset): NodeData {
    val nodeId = generateId()
    val node = NodeData(
        id = nodeId,
        title = "Set ${variable.name}",
        definitionKey = setVariableKey(variable.id),
        kind = NodeKind.Custom,
        headerColor = variable.type.color,
        pins = listOf(
            NodePin(generateId(), "Exec", PinType.Flow, PinDirection.Input, nodeId, true),
            NodePin(generateId(), variable.name, variable.type, PinDirection.Input, nodeId),
            NodePin(generateId(), "Then", PinType.Flow, PinDirection.Output, nodeId, true),
            NodePin(generateId(), variable.name, variable.type, PinDirection.Output, nodeId),
        ),
        position = position,
    )
    addNode(node)
    return node
}

internal fun parseVariableDefault(raw: String, type: PinType): Any? = when (type) {
    PinType.Float -> raw.toFloatOrNull() ?: 0f
    PinType.Integer -> raw.toIntOrNull() ?: 0
    PinType.Boolean -> raw.trim().lowercase() == "true"
    is PinType.String -> raw.ifEmpty { null }
    is PinType.Enum -> raw.ifEmpty { type.entries.firstOrNull() }
    PinType.Flow -> null
    is PinType.Wildcard -> null
    is PinType.Custom -> null
}

internal data class AddVariableCmd(val variable: GraphVariable, val defaultValue: Any? = null) : GraphCommand {
    override fun undo(state: GraphState) {
        state.variables.remove(variable)
        state.variableValues.remove(variable.id)
    }

    override fun redo(state: GraphState) {
        state.variables += variable
        if (defaultValue != null && defaultValue !is String && state.variableValues[variable.id] == null) {
            state.variableValues[variable.id] = defaultValue
        } else {
            state.variableValues[variable.id] = parseVariableDefault(variable.defaultValue, variable.type)
        }
    }
}

internal data class RemoveVariableCmd(val variable: GraphVariable) : GraphCommand {
    override fun undo(state: GraphState) {
        state.variables += variable
        state.variableValues[variable.id] = parseVariableDefault(variable.defaultValue, variable.type)
    }

    override fun redo(state: GraphState) {
        state.variables.remove(variable)
        state.variableValues.remove(variable.id)
    }
}
