package com.klyx.nodegraph.extension.builtin

import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.EvaluateScope
import com.klyx.nodegraph.InputPin
import com.klyx.nodegraph.NodeRegistry
import com.klyx.nodegraph.OutputPin
import com.klyx.nodegraph.PinType
import com.klyx.nodegraph.PureNode
import com.klyx.nodegraph.extension.GraphExtension

private object AppendStringNode : PureNode() {
    override val key = "builtin.append_string"
    override val title = "Append String"
    override val category = "String"
    override val description = "Concatenates A and B."
    override val headerColor = Color(0xFFFF8A65)
    override val compactTitle = "A + B"

    override val pins = listOf(
        InputPin("A", PinType.String()),
        InputPin("B", PinType.String()),
        OutputPin("Result", PinType.String()),
    )

    override fun EvaluateScope.evaluate() {
        outputs["Result"] = inputs.string("A") + inputs.string("B")
    }
}

private object StringLengthNode : PureNode() {
    override val key = "builtin.string_length"
    override val title = "String Length"
    override val category = "String"
    override val description = "Returns the number of characters in the string."
    override val headerColor = Color(0xFFFF8A65)

    override val pins = listOf(
        InputPin("Value", PinType.String()),
        OutputPin("Length", PinType.Integer),
    )

    override fun EvaluateScope.evaluate() {
        outputs["Length"] = inputs.string("Value").length
    }
}

private object StringContainsNode : PureNode() {
    override val key = "builtin.string_contains"
    override val title = "String Contains"
    override val category = "String"
    override val description = "Returns true if Value contains the Search string."
    override val headerColor = Color(0xFFFF8A65)

    override val pins = listOf(
        InputPin("Value", PinType.String()),
        InputPin("Search", PinType.String()),
        OutputPin("Result", PinType.Boolean),
    )

    override fun EvaluateScope.evaluate() {
        outputs["Result"] = inputs.string("Value").contains(inputs.string("Search"))
    }
}

private object StringToFloat : PureNode() {
    override val key = "builtin.string_to_float"
    override val title = "String to Float"
    override val category = "Conversion"
    override val description = "Converts String to Float."
    override val headerColor = Color(0xFFFF8A65)

    override val pins = listOf(
        InputPin("Value", PinType.String()),
        OutputPin("Result", PinType.Float),
        OutputPin("Success", PinType.Boolean)
    )

    override fun EvaluateScope.evaluate() {
        val parsed = inputs.string("Value").toFloatOrNull()
        outputs["Result"] = parsed ?: 0f
        outputs["Success"] = parsed != null
    }
}

private object StringToInt : PureNode() {
    override val key = "builtin.string_to_int"
    override val title = "String to Int"
    override val category = "Conversion"
    override val description = "Converts String to Int."
    override val headerColor = Color(0xFFFF8A65)

    override val pins = listOf(
        InputPin("Value", PinType.String()),
        OutputPin("Result", PinType.Integer),
        OutputPin("Success", PinType.Boolean)
    )

    override fun EvaluateScope.evaluate() {
        val parsed = inputs.string("Value").toIntOrNull()
        outputs["Result"] = parsed ?: 0
        outputs["Success"] = parsed != null
    }
}

private object StringEqualsNode : PureNode() {
    override val key = "core.string.equals"
    override val title = "String Equals"
    override val category = "String"
    override val description = "Checks if String A exactly matches String B."

    override val headerColor = Color(0xFF4CAF50)

    override val pins = listOf(
        InputPin("A", PinType.String()),
        InputPin("B", PinType.String()),
        InputPin("Ignore Case", PinType.Boolean, defaultValue = "true"),
        OutputPin("Result", PinType.Boolean)
    )

    override fun EvaluateScope.evaluate() {
        val a = inputs.string("A")
        val b = inputs.string("B")
        val ignoreCase = inputs.boolean("Ignore Case")

        outputs["Result"] = a.equals(b, ignoreCase = ignoreCase)
    }
}

internal object Strings : GraphExtension {
    override val name = "Strings"

    override fun install(registry: NodeRegistry) {
        registry.register(
            AppendStringNode, StringLengthNode, StringContainsNode,
            StringToFloat, StringToInt, StringEqualsNode
        )
    }
}
