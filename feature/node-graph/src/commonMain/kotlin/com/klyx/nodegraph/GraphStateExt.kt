package com.klyx.nodegraph

import androidx.compose.ui.geometry.Offset
import com.klyx.nodegraph.extension.Variable

fun GraphState.instantiate(node: Node, position: Offset) {
    addNode(node.instantiate(position))
}

fun GraphState.addNodes(vararg nodes: NodeData) {
    for (node in nodes) addNode(node)
}

fun GraphState.addVariable(variable: Variable, recordUndo: Boolean = true) {
    addVariable(
        id = variable.id,
        name = variable.name,
        type = variable.type,
        defaultValue = variable.defaultValue,
        isSystem = variable.isSystem,
        recordUndo = recordUndo
    )
}

@Suppress("UNCHECKED_CAST")
fun <T> GraphState.getVariable(name: String): T? {
    val variable = variables.find { it.name == name } ?: return null
    return variableValues[variable.id] as? T
}

fun GraphState.setVariable(name: String, value: Any?) {
    val variable = variables.find { it.name == name }
        ?: throw IllegalArgumentException("[Graph] Variable '$name' does not exist in the graph.")

    if (value != null) {
        val isValid = when (variable.type) {
            PinType.Integer -> value is Int
            PinType.Float -> value is Float
            PinType.Boolean -> value is Boolean
            PinType.String -> value is String
            is PinType.Custom -> {
                // for custom types, check if the class name matches the registered typeName
                // (handle both simple names and fully qualified names just in case)
                val className = value::class.simpleName
                className == variable.type.typeName || value::class.qualifiedName?.endsWith(variable.type.typeName) == true
            }

            else -> false
        }

        if (!isValid) {
            throw IllegalArgumentException(
                "[Graph] Type mismatch for variable '$name'. " +
                        "Expected '${variable.type.typeName}', but got '${value::class.simpleName}'."
            )
        }
    } else if (variable.type !is PinType.Custom && variable.type != PinType.String) {
        throw IllegalArgumentException("[Graph] Variable '$name' of type '${variable.type.typeName}' cannot be null.")
    }

    variableValues[variable.id] = value
}

