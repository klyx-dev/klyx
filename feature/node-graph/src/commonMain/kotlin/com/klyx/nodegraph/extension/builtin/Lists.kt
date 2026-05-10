package com.klyx.nodegraph.extension.builtin

import com.klyx.nodegraph.ActionNode
import com.klyx.nodegraph.EvaluateScope
import com.klyx.nodegraph.FlowScope
import com.klyx.nodegraph.InputHeaderPin
import com.klyx.nodegraph.InputPin
import com.klyx.nodegraph.NodeRegistry
import com.klyx.nodegraph.OutputHeaderPin
import com.klyx.nodegraph.OutputPin
import com.klyx.nodegraph.Pin
import com.klyx.nodegraph.PinType
import com.klyx.nodegraph.PureNode
import com.klyx.nodegraph.core.StandardNodeColors
import com.klyx.nodegraph.extension.GraphExtension
import kotlinx.coroutines.yield

private const val CATEGORY = "Lists"

private object CreateListNode : PureNode() {
    override val key = "builtin.create_list"
    override val title = "Create List"
    override val category = CATEGORY
    override val description = "Creates a list from connected input values."
    override val headerColor = StandardNodeColors.Headers.Variable

    override val pins = listOf(
        OutputPin("List", PinType.List(PinType.Wildcard()))
    )

    override val supportsDynamicPins = true
    override fun dynamicInputTemplate(): Pin = InputPin("Element 1", PinType.Wildcard())
    override val defaultDynamicInputCount = 2

    override fun inferOutputTypes(inputTypes: Map<String, PinType>): Map<String, PinType> {
        val elementType = inputTypes.values.firstOrNull { it !is PinType.Wildcard }
            ?: PinType.Wildcard()
        return mapOf("List" to PinType.List(elementType))
    }

    override fun EvaluateScope.evaluate() {
        val elements = inputs.labels
            .filter { it.startsWith("Element ") }
            .sortedBy { it.removePrefix("Element ").toIntOrNull() ?: 0 }
            .map { inputs.any(it) }
        outputs["List"] = elements
    }
}

private object ListGetNode : PureNode() {
    override val key = "builtin.list_get"
    override val title = "List Get"
    override val category = CATEGORY
    override val description = "Gets the element at the specified index from a list."
    override val headerColor = StandardNodeColors.Headers.Pure

    override val pins = listOf(
        InputPin("List", PinType.List(PinType.Wildcard())),
        InputPin("Index", PinType.Integer),
        OutputPin("Element", PinType.Wildcard())
    )

    override fun inferOutputTypes(inputTypes: Map<String, PinType>): Map<String, PinType> {
        val listType = inputTypes["List"]
        val elementType = if (listType is PinType.List) listType.elementType else PinType.Wildcard()
        return mapOf("Element" to elementType)
    }

    override fun EvaluateScope.evaluate() {
        val list = inputs.list("List")
        val index = inputs.int("Index")
        outputs["Element"] = list.getOrNull(index)
    }
}

private object ListSetNode : ActionNode() {
    override val key = "builtin.list_set"
    override val title = "List Set"
    override val category = CATEGORY
    override val description = "Sets the element at the specified index in a list."
    override val headerColor = StandardNodeColors.Headers.Action

    override val pins = listOf(
        InputHeaderPin("Exec"),
        InputPin("List", PinType.List(PinType.Wildcard())),
        InputPin("Index", PinType.Integer),
        InputPin("Value", PinType.Wildcard()),
        OutputHeaderPin("Then"),
        OutputPin("Result", PinType.List(PinType.Wildcard()))
    )

    override fun inferOutputTypes(inputTypes: Map<String, PinType>): Map<String, PinType> {
        val listType = inputTypes["List"]
        val elementType = if (listType is PinType.List) listType.elementType else PinType.Wildcard()
        return mapOf("Result" to PinType.List(elementType))
    }

    override suspend fun FlowScope.execute() {
        val list = inputs.list("List").toMutableList()
        val index = inputs.int("Index")
        val value = inputs.any("Value")
        if (index in list.indices) {
            list[index] = value
        }
        updateOutput("Result", list.toList())
        trigger("Then")
    }
}

private object ListAppendNode : ActionNode() {
    override val key = "builtin.list_append"
    override val title = "List Append"
    override val category = CATEGORY
    override val description = "Appends a value to a list."
    override val headerColor = StandardNodeColors.Headers.Action

    override val pins = listOf(
        InputHeaderPin("Exec"),
        InputPin("List", PinType.List(PinType.Wildcard())),
        InputPin("Value", PinType.Wildcard()),
        OutputHeaderPin("Then"),
        OutputPin("Result", PinType.List(PinType.Wildcard()))
    )

    override fun inferOutputTypes(inputTypes: Map<String, PinType>): Map<String, PinType> {
        val listType = inputTypes["List"]
        val elementType = if (listType is PinType.List) listType.elementType else PinType.Wildcard()
        return mapOf("Result" to PinType.List(elementType))
    }

    override suspend fun FlowScope.execute() {
        val list = inputs.list("List").toMutableList()
        list.add(inputs.any("Value"))
        updateOutput("Result", list.toList())
        trigger("Then")
    }
}

private object ListSizeNode : PureNode() {
    override val key = "builtin.list_size"
    override val title = "List Size"
    override val category = CATEGORY
    override val description = "Returns the number of elements in a list."
    override val headerColor = StandardNodeColors.Headers.Pure
    override val compactTitle = "Size"

    override val pins = listOf(
        InputPin("List", PinType.List(PinType.Wildcard())),
        OutputPin("Size", PinType.Integer),
    )

    override fun EvaluateScope.evaluate() {
        outputs["Size"] = inputs.list("List").size
    }
}

private object ListContainsNode : PureNode() {
    override val key = "builtin.list_contains"
    override val title = "List Contains"
    override val category = CATEGORY
    override val description = "Returns true if the value is in the list."
    override val headerColor = StandardNodeColors.Headers.Pure
    override val compactTitle = "Contains"

    override val pins = listOf(
        InputPin("List", PinType.List(PinType.Wildcard())),
        InputPin("Value", PinType.Wildcard()),
        OutputPin("Result", PinType.Boolean),
    )

    override fun EvaluateScope.evaluate() {
        val list = inputs.list("List")
        val value = inputs.any("Value")
        outputs["Result"] = list.contains(value)
    }
}

private object ListRemoveAtNode : ActionNode() {
    override val key = "builtin.list_remove_at"
    override val title = "List Remove At"
    override val category = CATEGORY
    override val description = "Removes the element at the specified index from a list."
    override val headerColor = StandardNodeColors.Headers.Action

    override val pins = listOf(
        InputHeaderPin("Exec"),
        InputPin("List", PinType.List(PinType.Wildcard())),
        InputPin("Index", PinType.Integer),
        OutputHeaderPin("Then"),
        OutputPin("Result", PinType.List(PinType.Wildcard()))
    )

    override fun inferOutputTypes(inputTypes: Map<String, PinType>): Map<String, PinType> {
        val listType = inputTypes["List"]
        val elementType = if (listType is PinType.List) listType.elementType else PinType.Wildcard()
        return mapOf("Result" to PinType.List(elementType))
    }

    override suspend fun FlowScope.execute() {
        val list = inputs.list("List").toMutableList()
        val index = inputs.int("Index")
        if (index in list.indices) {
            list.removeAt(index)
        }
        updateOutput("Result", list.toList())
        trigger("Then")
    }
}

private object ForEachNode : ActionNode() {
    override val key = "builtin.for_each"
    override val title = "For Each"
    override val category = "Flow Control"
    override val description = "Iterates over each element in a list and fires the loop body."
    override val headerColor = StandardNodeColors.Headers.ControlFlow

    override val pins = listOf(
        InputHeaderPin("Exec"),
        InputPin("List", PinType.List(PinType.Wildcard())),
        OutputPin("Loop Body", PinType.Flow),
        OutputPin("Element", PinType.Wildcard()),
        OutputPin("Index", PinType.Integer),
        OutputPin("Completed", PinType.Flow)
    )

    override fun inferOutputTypes(inputTypes: Map<String, PinType>): Map<String, PinType> {
        val listType = inputTypes["List"]
        val elementType = if (listType is PinType.List) listType.elementType else PinType.Wildcard()
        return mapOf("Element" to elementType)
    }

    override suspend fun FlowScope.execute() {
        val list = inputs.list("List")

        for ((index, element) in list.withIndex()) {
            updateOutput("Element", element)
            updateOutput("Index", index)
            resetEvaluationCache()

            try {
                trigger("Loop Body")
            } catch (_: BreakSignal) {
                break
            } catch (_: ContinueSignal) {
                continue
            }

            yield()
        }

        trigger("Completed")
    }
}

private object ListToArrayNode : PureNode() {
    override val key = "builtin.list_to_array"
    override val title = "List to Array"
    override val category = CATEGORY
    override val description = "Converts a list to an array."
    override val headerColor = StandardNodeColors.Headers.Variable

    override val pins = listOf(
        InputPin("List", PinType.List(PinType.Wildcard())),
        OutputPin("Array", PinType.Array(PinType.Wildcard()))
    )

    override fun inferOutputTypes(inputTypes: Map<String, PinType>): Map<String, PinType> {
        val listType = inputTypes["List"]
        val elementType = if (listType is PinType.List) listType.elementType else PinType.Wildcard()
        return mapOf("Array" to PinType.Array(elementType))
    }

    @Suppress("UNCHECKED_CAST")
    override fun EvaluateScope.evaluate() {
        val list = inputs.list("List")
        outputs["Array"] = list.toTypedArray()
    }
}

private object ArrayToListNode : PureNode() {
    override val key = "builtin.array_to_list"
    override val title = "Array to List"
    override val category = CATEGORY
    override val description = "Converts an array to a list."
    override val headerColor = StandardNodeColors.Headers.Variable

    override val pins = listOf(
        InputPin("Array", PinType.Array(PinType.Wildcard())),
        OutputPin("List", PinType.List(PinType.Wildcard()))
    )

    override fun inferOutputTypes(inputTypes: Map<String, PinType>): Map<String, PinType> {
        val arrayType = inputTypes["Array"]
        val elementType = if (arrayType is PinType.Array) arrayType.elementType else PinType.Wildcard()
        return mapOf("List" to PinType.List(elementType))
    }

    override fun EvaluateScope.evaluate() {
        val array = inputs.array("Array")
        outputs["List"] = array.toList()
    }
}

internal object Lists : GraphExtension {
    override val name = "Lists"

    override fun install(registry: NodeRegistry) {
        registry.register(
            CreateListNode,
            ListGetNode,
            ListSetNode,
            ListAppendNode,
            ListSizeNode,
            ListContainsNode,
            ListRemoveAtNode,
            ForEachNode,
            ListToArrayNode,
            ArrayToListNode,
        )
    }
}
